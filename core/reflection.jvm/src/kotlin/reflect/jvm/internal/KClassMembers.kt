/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.builtins.jvm.JvmBuiltInsSignatures
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.runtime.structure.classId
import org.jetbrains.kotlin.descriptors.runtime.structure.wrapperByPrimitive
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.java.getPropertyNamesCandidatesByAccessorName
import org.jetbrains.kotlin.load.kotlin.SignatureBuildingComponents
import org.jetbrains.kotlin.load.kotlin.internalName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.utils.DFS
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import kotlin.jvm.internal.CallableReference.NO_RECEIVER
import kotlin.metadata.ClassKind
import kotlin.metadata.kind
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.superclasses
import kotlin.reflect.full.valueParameters
import kotlin.reflect.jvm.internal.MemberBelonginess.DECLARED
import kotlin.reflect.jvm.internal.MemberBelonginess.INHERITED
import kotlin.reflect.jvm.internal.types.MutableCollectionKClass
import kotlin.reflect.jvm.internal.types.areEqualKTypes
import java.lang.Deprecated as JavaLangDeprecated

private const val ENUM_ENTRIES_PROPERTY_NAME = "entries"

internal fun KClassImpl<*>.computeDeclaredMembers(): Collection<ReflectKCallable<*>> =
    data.value.declaredMemberNames.flatMap(data.value::getDeclaredMembersByName)

internal fun KClassImpl<*>.computeAllMembers(): Collection<ReflectKCallable<*>> {
    val names: Collection<String> =
        if (!newFakeOverridesImplementation || useK1Implementation || isComplicatedBuiltinSubclass) {
            getMemberNamesFromDescriptors()
        } else buildSet {
            // All member names of this class are the _declared_ member names of this class plus declared member names of all its direct and
            // indirect supertypes. We can't obtain names from each supertype's `members`, because those have already had inherited statics
            // and cross-package package-private members filtered out, even though such members may still be visible in a subclass (e.g. a
            // Java static method inherited through a Kotlin class).
            collectDeclaredMemberNamesTransitively(this, hashSetOf())
        }
    return names.flatMap(data.value::getMembersByName)
}

private fun KClassImpl<*>.collectDeclaredMemberNamesTransitively(result: MutableSet<String>, visited: MutableSet<KClassImpl<*>>) {
    if (!visited.add(this)) return
    result.addAll(data.value.declaredMemberNames)
    for (supertype in supertypes) {
        (supertype.classifier as? KClassImpl<*>)?.collectDeclaredMemberNamesTransitively(result, visited)
    }
}

internal fun KClassImpl<*>.computeDeclaredMembersByName(name: String): Collection<ReflectKCallable<*>> = buildList {
    val kClass = this@computeDeclaredMembersByName
    if (useK1Implementation || isComplicatedBuiltinSubclass) {
        addAll(getDescriptorBasedMembers(memberScope, DECLARED, name))
        addAll(getDescriptorBasedMembers(staticScope, DECLARED, name))
    } else if (kmClass != null) {
        val kmClass = kmClass!!
        for (function in kmClass.functions) {
            if (function.name == name) {
                add(createUnboundFunction(function, kClass, kmClass))
            }
        }
        if (kmClass.kind == ClassKind.ENUM_CLASS) {
            if (name == StandardNames.ENUM_VALUES.asString()) {
                add(createUnboundFunction(createEnumValuesKmFunction(kClass), kClass, kmClass))
            }
            if (name == StandardNames.ENUM_VALUE_OF.asString()) {
                add(createUnboundFunction(createEnumValueOfKmFunction(kClass), kClass, kmClass))
            }
        }
        data.value.additionalFunctions.filterTo(this) { it.name == name }
        getDescriptorBasedMembers(memberScope, DECLARED, name).filterTo(this) { it is KProperty<*> }
        getDescriptorBasedMembers(staticScope, DECLARED, name).filterTo(this) { it is KProperty<*> }
    } else {
        getDeclaredNonStaticMethodsFromJavaClass(name).filterTo(this) { isVisibleAsFunctionInCurrentClass(it) }
        getDescriptorBasedMembers(memberScope, DECLARED, name).filterTo(this) { it is KProperty<*> }
        for (method in jClass.declaredMethods) {
            if (method.name == name && Modifier.isStatic(method.modifiers) && !method.isSynthetic) {
                add(JavaKNamedFunction(kClass, method, NO_RECEIVER, KCallableOverriddenStorage.EMPTY))
            }
        }

        for (field in jClass.declaredFields) {
            if (field.isEnumConstant) continue
            if (field.name == name && Modifier.isStatic(field.modifiers) && !field.isSynthetic) {
                if (Modifier.isFinal(field.modifiers)) {
                    add(JavaKProperty0<Any>(kClass, field, NO_RECEIVER, KCallableOverriddenStorage.EMPTY))
                } else {
                    add(JavaKMutableProperty0<Any>(kClass, field, NO_RECEIVER, KCallableOverriddenStorage.EMPTY))
                }
            }
        }

        if (jClass.isEnum && name == ENUM_ENTRIES_PROPERTY_NAME) {
            @Suppress("UNCHECKED_CAST")
            add(JavaEnumEntriesKProperty(kClass as KClassImpl<out Enum<*>>))
        }
    }
}

internal fun KClassImpl<*>.computeMembersByName(name: String): Collection<ReflectKCallable<*>> =
    if (!newFakeOverridesImplementation || useK1Implementation || isComplicatedBuiltinSubclass) {
        buildList {
            addAll(data.value.getDeclaredMembersByName(name))
            addAll(getDescriptorBasedMembers(memberScope, INHERITED, name))
            addAll(getDescriptorBasedMembers(staticScope, INHERITED, name))
        }
    } else {
        val isKotlin = java.isKotlin
        val members = getFakeOverrideMembersByName(name).filterNot { (_, member) ->
            // Kotlin classes never inherit static members (neither from Java, nor from Kotlin).
            (isKotlin && member.isStatic && member.overriddenStorage.isFakeOverride) ||
                    (member.isPackagePrivate && member.originalContainer.jClass.`package` != java.`package`)
        }
        members.values + data.value.getDeclaredMembersByName(name).filter { isNonTransitiveMember(this, it) }
    }

internal fun KClassImpl<*>.computeDeclaredMemberNames(): Set<String> =
    if (useK1Implementation || isComplicatedBuiltinSubclass) {
        getMemberNamesFromDescriptors()
    } else if (kmClass != null) buildSet {
        for (function in kmClass!!.functions) {
            add(function.name)
        }
        if (kmClass!!.kind == ClassKind.ENUM_CLASS) {
            add(StandardNames.ENUM_VALUES.asString())
            add(StandardNames.ENUM_VALUE_OF.asString())
        }
        data.value.additionalFunctions.mapTo(this, ReflectKCallable<*>::name)
        memberScope.getVariableNames().mapTo(this, Name::asString)
        staticScope.getVariableNames().mapTo(this, Name::asString)
    } else buildSet {
        if (!jClass.isAnnotation) {
            for (method in jClass.declaredMethods) {
                if (!method.isSynthetic) add(method.name)
            }
        }
        for (field in jClass.declaredFields) {
            if (!field.isEnumConstant && Modifier.isStatic(field.modifiers) && !field.isSynthetic) add(field.name)
        }
        memberScope.getVariableNames().mapTo(this, Name::asString)
        if (jClass.isEnum) {
            add(ENUM_ENTRIES_PROPERTY_NAME)
        }
    }

private fun KClassImpl<*>.getMemberNamesFromDescriptors(): Set<String> = buildSet {
    memberScope.getFunctionNames().mapTo(this, Name::asString)
    memberScope.getVariableNames().mapTo(this, Name::asString)
    staticScope.getFunctionNames().mapTo(this, Name::asString)
    staticScope.getVariableNames().mapTo(this, Name::asString)
}

private fun KClassImpl<*>.getDescriptorBasedMembers(
    scope: MemberScope, belonginess: MemberBelonginess, name: String,
): Collection<DescriptorKCallable<*>> {
    val visitor = object : CreateKCallableVisitor(this) {
        override fun visitConstructorDescriptor(descriptor: ConstructorDescriptor, data: Unit): DescriptorKCallable<*> =
            throw IllegalStateException("No constructors should appear here: $descriptor")
    }
    val identifier = Name.identifier(name)
    return (scope.getContributedFunctions(identifier, NoLookupLocation.FROM_REFLECTION) +
            scope.getContributedVariables(identifier, NoLookupLocation.FROM_REFLECTION)).mapNotNull { descriptor ->
        if (descriptor.visibility != DescriptorVisibilities.INVISIBLE_FAKE && belonginess.accept(descriptor))
            descriptor.accept(visitor, Unit) else null
    }
}

private enum class MemberBelonginess {
    DECLARED,
    INHERITED;

    fun accept(member: CallableMemberDescriptor): Boolean =
        member.kind.isReal == (this == DECLARED)
}

internal fun KClassImpl<*>.isVisibleAsFunctionInCurrentClass(function: JavaKNamedFunction): Boolean {
    if (getPropertyNamesCandidatesByAccessorName(Name.identifier(function.name)).any { propertyName ->
            getPropertiesFromSupertypes(propertyName.asString()).any { property ->
                doesClassOverrideProperty(property) { accessorName ->
                    if (function.name == accessorName)
                        listOf(function)
                    else {
                        // K1 code also searched in supertypes (see searchMethodsInSupertypesWithoutBuiltinMagic), but it seems useful
                        // only for mapped builtins and their subtypes, so will be handled separately in KT-85727.
                        getDeclaredNonStaticMethodsFromJavaClass().filter { it.name == accessorName }
                    }
                } && (property is KMutableProperty<*> || !JvmAbi.isSetterName(function.name))
            }
        }) return false

    return true
}

internal fun KClassImpl<*>.getDeclaredNonStaticMethodsFromJavaClass(name: String? = null): List<JavaKNamedFunction> {
    require(kmClass == null) { "Should be called only for Java classes: $this" }
    if (jClass.isAnnotation) return emptyList()
    return jClass.declaredMethods.mapNotNull { method ->
        if ((name != null && method.name != name) || Modifier.isStatic(method.modifiers) || method.isSynthetic) null
        else JavaKNamedFunction(this, method, NO_RECEIVER, KCallableOverriddenStorage.EMPTY)
    }
}

private fun KClassImpl<*>.getPropertiesFromSupertypes(name: String): List<KProperty1<*, *>> =
    supertypes.flatMap { supertype -> (supertype.classifier as? KClass<*>)?.memberProperties?.filter { it.name == name }.orEmpty() }

private fun doesClassOverrideProperty(
    property: KProperty1<*, *>,
    functions: (String) -> Collection<ReflectKFunction>,
): Boolean {
    // Java fields cannot be overridden.
    if (property is JavaKProperty<*>) return false

    val getter = property.findGetterOverride(functions)
    val setter = property.findSetterOverride(functions)

    if (getter == null) return false
    if (property !is KMutableProperty<*>) return true

    return setter != null && setter.modality == getter.modality
}

private fun KProperty1<*, *>.findGetterOverride(functions: (String) -> Collection<ReflectKFunction>): ReflectKFunction? =
    findGetterByName(JvmAbi.getterName(name), functions)

private fun KProperty1<*, *>.findGetterByName(
    getterName: String,
    functions: (String) -> Collection<ReflectKFunction>,
): ReflectKFunction? =
    functions(getterName).firstOrNull { function ->
        function.valueParameters.isEmpty() && function.returnType.isSubtypeOf(returnType)
    }

private fun KProperty1<*, *>.findSetterOverride(
    functions: (String) -> Collection<ReflectKFunction>,
): ReflectKFunction? =
    functions(JvmAbi.setterName(name)).firstOrNull { function ->
        val valueParameters = function.valueParameters
        valueParameters.size == 1 && function.returnType == StandardKTypes.UNIT_RETURN_TYPE &&
                areEqualKTypes(valueParameters.single().type, returnType)
    }

// Additional functions are the Java methods of a built-in class's Java analogue that should be visible on the Kotlin class but are not
// declared in its metadata. This is the reflection counterpart of `JvmBuiltInsCustomizer.getAdditionalFunctions`.
internal fun KClassImpl<*>.getAdditionalFunctions(): List<ReflectKFunction> {
    if (!isMappedBuiltin || this == Any::class) return emptyList()
    val kmClass = kmClass ?: return emptyList()

    val javaAnalogue = jClass.wrapperByPrimitive ?: jClass
    val isMutable = JavaToKotlinClassMap.isMutable(classId)

    // Property accessors must not be loaded as functions; the compiler filters them out because they override the corresponding
    // property accessors declared in this class. Unlike functions (handled below), reflection keeps properties and functions separate,
    // so they are not deduplicated against each other automatically.
    val getterLikeNames = HashSet<String>()   // matched against 0-arg methods, e.g. Enum.name()/ordinal() and Throwable.getMessage()
    val setterLikeNames = HashSet<String>()   // matched against 1-arg methods
    for (property in kmClass.properties) {
        getterLikeNames += property.name
        getterLikeNames += JvmAbi.getterName(property.name)
        setterLikeNames += JvmAbi.setterName(property.name)
    }

    // JVM signatures of functions declared in this class's metadata, used to avoid replacing a Kotlin function (which has proper
    // Kotlin types) with a Java method (which has flexible types), e.g. `Enum.clone`.
    val declaredJvmSignatures = kmClass.functions.mapTo(HashSet()) {
        it.mapSignature(this, kmClass).toString()
    }

    // JVM signatures of functions declared in the mutable counterpart of this read-only class (e.g. `MutableIterator.remove`,
    // `MutableListIterator.add`/`set`). Such mutating methods must not be loaded on the read-only class; the mutable variant gets them
    // from its own metadata. This is the reflection equivalent of the supertype DFS in `JvmBuiltInsCustomizer.isMutabilityViolation`.
    val mutableOnlySignatures = if (isMutable) emptySet() else collectMutableCounterpartFunctionSignatures()

    return javaAnalogue.declaredMethods.mapNotNull { method ->
        if (Modifier.isStatic(method.modifiers) || method.isSynthetic) return@mapNotNull null
        if (!Modifier.isPublic(method.modifiers) && !Modifier.isProtected(method.modifiers)) return@mapNotNull null
        if (method.isAnnotationPresent(JavaLangDeprecated::class.java)) return@mapNotNull null

        val parameterCount = method.parameterTypes.size
        if (parameterCount == 0 && method.name in getterLikeNames) return@mapNotNull null
        if (parameterCount == 1 && method.name in setterLikeNames) return@mapNotNull null

        if (method.isMutabilityViolation(isMutable) || method.jvmSignature in mutableOnlySignatures) return@mapNotNull null

        when (method.getJdkMethodStatus(javaAnalogue)) {
            JdkMemberStatus.DROP -> return@mapNotNull null
            // Hidden-for-resolution members are still listed by reflection, except in final classes where the compiler drops them.
            JdkMemberStatus.HIDDEN -> if (isFinal) return@mapNotNull null
            JdkMemberStatus.VISIBLE, JdkMemberStatus.DEPRECATED_LIST_METHODS, JdkMemberStatus.NOT_CONSIDERED -> {}
        }

        val function = JavaKNamedFunction(this, method, NO_RECEIVER, KCallableOverriddenStorage.EMPTY)

        // Skip a Java method if it corresponds to a function already present in the Kotlin class: either declared in its metadata, or
        // inherited from a supertype (e.g. `equals`/`hashCode`/`toString` from `kotlin.Any`, or `compareTo` from `Comparable`).
        // Otherwise the Java-based function, which has flexible types (`equals(Any!)` instead of `equals(Any?)`), would replace the
        // Kotlin one. This mirrors the `kotlinVersions` check in `JvmBuiltInsCustomizer.getAdditionalFunctions`.
        if (method.jvmSignature in declaredJvmSignatures) return@mapNotNull null
        if (function.overridden.isNotEmpty()) return@mapNotNull null

        function
    }
}

private enum class JdkMemberStatus { HIDDEN, VISIBLE, DEPRECATED_LIST_METHODS, NOT_CONSIDERED, DROP }

// Mirrors `JvmBuiltInsCustomizer.getJdkMethodStatus`: walk the analogue's supertypes (which are themselves Java analogues) and match the
// method signature against the JDK member lists; the first match wins.
private fun Method.getJdkMethodStatus(startClass: Class<*>): JdkMemberStatus {
    val jvmDescriptor = jvmSignature
    val visited = HashSet<Class<*>>()
    val queue = ArrayDeque<Class<*>>().apply { add(startClass) }
    while (queue.isNotEmpty()) {
        val clazz = queue.removeFirst()
        if (!visited.add(clazz)) continue
        when (SignatureBuildingComponents.signature(clazz.classId.internalName, jvmDescriptor)) {
            in JvmBuiltInsSignatures.HIDDEN_METHOD_SIGNATURES -> return JdkMemberStatus.HIDDEN
            in JvmBuiltInsSignatures.VISIBLE_METHOD_SIGNATURES -> return JdkMemberStatus.VISIBLE
            in JvmBuiltInsSignatures.DEPRECATED_LIST_METHODS -> return JdkMemberStatus.DEPRECATED_LIST_METHODS
            in JvmBuiltInsSignatures.DROP_LIST_METHOD_SIGNATURES -> return JdkMemberStatus.DROP
        }
        clazz.superclass?.let(queue::add)
        queue.addAll(clazz.interfaces)
    }
    return JdkMemberStatus.NOT_CONSIDERED
}

// Mirrors the signature-list half of `JvmBuiltInsCustomizer.isMutabilityViolation`: a mutating method (`List.sort`, `Collection.removeIf`,
// ...) belongs only on the mutable variant of a collection, and vice versa. Methods declared in the mutable Kotlin classes themselves
// (`MutableIterator.remove` etc.) are handled separately via `collectMutableCounterpartFunctionSignatures`.
private fun Method.isMutabilityViolation(isMutable: Boolean): Boolean =
    (SignatureBuildingComponents.signature(declaringClass.classId.internalName, jvmSignature)
            in JvmBuiltInsSignatures.MUTABLE_METHOD_SIGNATURES) != isMutable

// JVM signatures of all functions declared in this read-only class's mutable counterpart and its mutable supertypes.
private fun KClassImpl<*>.collectMutableCounterpartFunctionSignatures(): Set<String> {
    val mutableClass = getMutableCollectionKClass(this) ?: return emptySet()
    return DFS.dfs(
        listOf(mutableClass),
        KClass<*>::superclasses,
        object : DFS.CollectingNodeHandler<KClass<*>, String, HashSet<String>>(HashSet()) {
            override fun beforeChildren(node: KClass<*>): Boolean {
                val mutableKmClass = (node as? MutableCollectionKClass<*>)?.mutableKmClass ?: return true
                for (function in mutableKmClass.functions) {
                    result += function.mapSignature(this@collectMutableCounterpartFunctionSignatures, mutableKmClass).toString()
                }
                return true
            }
        },
    )
}
