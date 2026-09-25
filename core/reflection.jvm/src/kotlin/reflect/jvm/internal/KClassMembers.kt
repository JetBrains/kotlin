/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.builtins.jvm.JvmBuiltInsSignatures
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.runtime.structure.classId
import org.jetbrains.kotlin.descriptors.runtime.structure.wrapperByPrimitive
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.load.java.BuiltinSpecialProperties
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.java.SpecialGenericSignatures
import org.jetbrains.kotlin.load.java.getPropertyNamesCandidatesByAccessorName
import org.jetbrains.kotlin.load.kotlin.SignatureBuildingComponents
import org.jetbrains.kotlin.load.kotlin.internalName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import kotlin.jvm.internal.CallableReference.NO_RECEIVER
import kotlin.metadata.ClassKind
import kotlin.metadata.KmClass
import kotlin.metadata.KmClassifier
import kotlin.metadata.jvm.JvmMethodSignature
import kotlin.metadata.kind
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.valueParameters
import kotlin.reflect.jvm.internal.MemberBelonginess.DECLARED
import kotlin.reflect.jvm.internal.MemberBelonginess.INHERITED
import kotlin.reflect.jvm.internal.types.MutableCollectionKClass
import kotlin.reflect.jvm.internal.types.areEqualKTypes
import java.lang.Deprecated as JavaLangDeprecated

private const val ENUM_ENTRIES_PROPERTY_NAME = "entries"

internal fun MemberContainer<*>.computeDeclaredMembers(): Collection<ReflectKCallable<*>> =
    declaredMemberNames.flatMap(this::getDeclaredMembersByName)

internal fun MemberContainer<*>.computeAllMembers(): Collection<ReflectKCallable<*>> {
    val names: Collection<String> =
        if (this is KClassImpl<*> && (useK1Implementation || isComplicatedBuiltinSubclass)) {
            getMemberNamesFromDescriptors()
        } else buildSet {
            // All member names of this class are the _declared_ member names of this class plus declared member names of all its direct and
            // indirect supertypes. We can't obtain names from each supertype's `members`, because those have already had inherited statics
            // and cross-package package-private members filtered out, even though such members may still be visible in a subclass (e.g. a
            // Java static method inherited through a Kotlin class).
            collectDeclaredMemberNamesTransitively(this, hashSetOf())
        }
    return names.flatMap(this::getMembersByName)
}

private fun MemberContainer<*>.collectDeclaredMemberNamesTransitively(
    result: MutableSet<String>,
    visited: MutableSet<MemberContainer<*>>,
) {
    if (!visited.add(this)) return
    result.addAll(declaredMemberNames)
    for (supertype in supertypes) {
        supertype.memberContainer?.collectDeclaredMemberNamesTransitively(result, visited)
    }
}

internal fun KClassImpl<*>.computeDeclaredMembersByName(name: String): Collection<ReflectKCallable<*>> = buildList {
    val kClass = this@computeDeclaredMembersByName
    if (useK1Implementation || isComplicatedBuiltinSubclass || (useK1ImplementationForMembers && kmClass != null)) {
        addAll(getDescriptorBasedFunctions(memberScope, DECLARED, name))
        addAll(getDescriptorBasedProperties(memberScope, DECLARED, name))
        addAll(getDescriptorBasedFunctions(staticScope, DECLARED, name))
        addAll(getDescriptorBasedProperties(staticScope, DECLARED, name))
    } else if (kmClass != null) {
        addAll(computeDeclaredMembersFromMetadata(name))
    } else {
        getDeclaredNonStaticMethodsFromJavaClass(name).filterTo(this) { isVisibleAsFunctionInCurrentClass(it) }
        addOverriddenSpecialMethods(name, this)
        if (useK1ImplementationForMembers) {
            addAll(getDescriptorBasedProperties(memberScope, DECLARED, name))
        }
        for (method in jClass.declaredMethods) {
            if (method.name == name && Modifier.isStatic(method.modifiers) && !method.isSynthetic) {
                add(JavaKNamedFunction(kClass, method, NO_RECEIVER, KCallableOverriddenStorage.EMPTY))
            }
        }

        for (field in jClass.declaredFields) {
            if (field.isEnumConstant || field.isSynthetic || field.name != name) continue
            if (useK1ImplementationForMembers && !Modifier.isStatic(field.modifiers)) continue
            when {
                Modifier.isStatic(field.modifiers) -> when {
                    Modifier.isFinal(field.modifiers) ->
                        add(JavaFieldKProperty0<Any?>(kClass, field, NO_RECEIVER, KCallableOverriddenStorage.EMPTY))
                    else ->
                        add(JavaFieldKMutableProperty0<Any?>(kClass, field, NO_RECEIVER, KCallableOverriddenStorage.EMPTY))
                }
                else -> when {
                    Modifier.isFinal(field.modifiers) ->
                        add(JavaFieldKProperty1<Any?, Any?>(kClass, field, NO_RECEIVER, KCallableOverriddenStorage.EMPTY))
                    else ->
                        add(JavaFieldKMutableProperty1<Any?, Any?>(kClass, field, NO_RECEIVER, KCallableOverriddenStorage.EMPTY))
                }
            }
        }

        if (!useK1ImplementationForMembers) {
            val propertiesFromSupertypes = getPropertiesFromSupertypes(name)
            if (propertiesFromSupertypes.isNotEmpty()) {
                val handledProperties = hashSetOf<KProperty1<*, *>>()
                addPropertyOverrideByMethod(propertiesFromSupertypes, this, handledProperties) {
                    getDeclaredNonStaticMethodsFromJavaClass(it)
                }
                // K1 also had logic about properties in supertypes, see `LazyJavaClassMemberScope.computeNonDeclaredProperties`.
                // However, the only test where it can be observed
                // `compiler/testData/ir/irText/firProblems/TypeParameterInClashingAccessor.kt` never worked in K1 reflection because of
                // other problems: KT-81029.
            }
        }

        if (jClass.isEnum && name == ENUM_ENTRIES_PROPERTY_NAME) {
            @Suppress("UNCHECKED_CAST")
            add(JavaEnumEntriesKProperty(kClass as KClassImpl<out Enum<*>>))
        }

        if (jClass.isAnnotation && !useK1ImplementationForMembers) {
            for (method in jClass.declaredMethods) {
                if (method.name == name && !method.isSynthetic) {
                    add(JavaAnnotationMethodKProperty1<Any?, Any?>(kClass, method, NO_RECEIVER, KCallableOverriddenStorage.EMPTY))
                }
            }
        }
    }
}

internal fun MemberContainer<*>.computeDeclaredMembersFromMetadata(name: String): Collection<ReflectKCallable<*>> = buildList {
    val kClass = this@computeDeclaredMembersFromMetadata
    val kmClass = kmClass ?: throw KotlinReflectionInternalError("Should be called only for Kotlin classes: $kClass")
    for (function in kmClass.functions) {
        if (function.name == name) {
            add(createUnboundFunction(function, kClass))
        }
    }
    for (property in kmClass.properties) {
        if (property.name == name) {
            add(createUnboundProperty(property, kClass))
        }
    }
    if (kmClass.kind == ClassKind.ENUM_CLASS) {
        when (name) {
            StandardNames.ENUM_VALUES.asString() -> add(createUnboundFunction(createEnumValuesKmFunction(kClass), kClass))
            StandardNames.ENUM_VALUE_OF.asString() -> add(createUnboundFunction(createEnumValueOfKmFunction(kClass), kClass))
            StandardNames.ENUM_ENTRIES.asString() -> add(createUnboundProperty(createEnumEntriesKmProperty(kClass), kClass))
        }
    }
    additionalFunctions.filterTo(this) { it.name == name }
}

internal fun MemberContainer<*>.computeMembersByName(name: String): Collection<ReflectKCallable<*>> =
    if (this is KClassImpl<*> && (useK1Implementation || isComplicatedBuiltinSubclass)) {
        buildList {
            addAll(getDeclaredMembersByName(name))
            addAll(getDescriptorBasedFunctions(memberScope, INHERITED, name))
            addAll(getDescriptorBasedProperties(memberScope, INHERITED, name))
            addAll(getDescriptorBasedFunctions(staticScope, INHERITED, name))
            addAll(getDescriptorBasedProperties(staticScope, INHERITED, name))
        }
    } else {
        val isKotlin = isKotlin
        val members = getFakeOverrideMembersByName(name).filterNot { (_, member) ->
            // Kotlin classes never inherit static members (neither from Java, nor from Kotlin).
            (isKotlin && member.isStatic && member.overriddenStorage.isFakeOverride) ||
                    (member.isPackagePrivate && member.originalContainer.jClass.`package` != java.`package`)
        }
        members.values + getDeclaredMembersByName(name).filter { isNonTransitiveMember(this, it) }
    }

internal fun KClassImpl<*>.computeDeclaredMemberNames(): Set<String> =
    if (useK1Implementation || isComplicatedBuiltinSubclass) {
        getMemberNamesFromDescriptors()
    } else if (kmClass != null) {
        computeDeclaredMemberNamesFromMetadata()
    } else buildSet {
        for (method in jClass.declaredMethods) {
            if (!method.isSynthetic) add(method.name)
        }
        for (field in jClass.declaredFields) {
            if (!field.isEnumConstant && !field.isSynthetic) add(field.name)
        }
        val visited = hashSetOf<MemberContainer<*>>()
        for (supertype in supertypes) {
            // Declared method `getX` in a Java class won't be loaded as a KFunction if it overrides a property getter from the base Kotlin
            // class (see `isVisibleAsFunctionInCurrentClass`), it will be loaded as a property getter instead. We cannot deduce the name
            // of the property from the name of the getter (`getX` -> `x` or `X`?), so we get all property names from supertypes.
            supertype.memberContainer?.collectDeclaredMemberNamesTransitively(this, visited)
        }
        if (jClass.isEnum) {
            add(ENUM_ENTRIES_PROPERTY_NAME)
        }
    }

internal fun MemberContainer<*>.computeDeclaredMemberNamesFromMetadata(): Set<String> = buildSet {
    val kmClass = kmClass ?: throw KotlinReflectionInternalError("Should be called only for Kotlin classes: $this")
    for (function in kmClass.functions) {
        add(function.name)
    }
    for (property in kmClass.properties) {
        add(property.name)
    }
    if (kmClass.kind == ClassKind.ENUM_CLASS) {
        add(StandardNames.ENUM_VALUES.asString())
        add(StandardNames.ENUM_VALUE_OF.asString())
        add(StandardNames.ENUM_ENTRIES.asString())
    }
    additionalFunctions.mapTo(this, ReflectKCallable<*>::name)
}

private fun KClassImpl<*>.getMemberNamesFromDescriptors(): Set<String> = buildSet {
    memberScope.getFunctionNames().mapTo(this, Name::asString)
    memberScope.getVariableNames().mapTo(this, Name::asString)
    staticScope.getFunctionNames().mapTo(this, Name::asString)
    staticScope.getVariableNames().mapTo(this, Name::asString)
}

private fun KClassImpl<*>.getDescriptorBasedFunctions(
    scope: MemberScope, belonginess: MemberBelonginess, name: String,
): Collection<DescriptorKFunction> =
    scope.getContributedFunctions(Name.identifier(name), NoLookupLocation.FROM_REFLECTION).createCallables(this, belonginess)

private fun KClassImpl<*>.getDescriptorBasedProperties(
    scope: MemberScope, belonginess: MemberBelonginess, name: String,
): Collection<DescriptorKProperty<*>> =
    scope.getContributedVariables(Name.identifier(name), NoLookupLocation.FROM_REFLECTION).createCallables(this, belonginess)

private inline fun <reified T : DescriptorKCallable<*>> Collection<CallableMemberDescriptor>.createCallables(
    container: KClassImpl<*>, belonginess: MemberBelonginess,
): List<T> = mapNotNull { descriptor ->
    if (descriptor.visibility != DescriptorVisibilities.INVISIBLE_FAKE && belonginess.accept(descriptor))
        descriptor.accept(CreateNonConstructorKCallableVisitor(container), Unit) as T else null
}

private enum class MemberBelonginess {
    DECLARED,
    INHERITED;

    fun accept(member: CallableMemberDescriptor): Boolean =
        member.kind.isReal == (this == DECLARED)
}

internal fun MemberContainer<*>.isVisibleAsFunctionInCurrentClass(function: JavaKNamedFunction): Boolean {
    if (getPropertyNamesCandidatesByAccessorName(Name.identifier(function.name)).any { propertyName ->
            getPropertiesFromSupertypes(propertyName.asString()).any { property ->
                doesClassOverrideProperty(property) { accessorName ->
                    if (function.name == accessorName)
                        listOf(function)
                    else {
                        // K1 code also searched in supertypes (see searchMethodsInSupertypesWithoutBuiltinMagic), but it seems useful
                        // only for mapped builtins and their subtypes, so will be handled separately in KT-85727.
                        getDeclaredNonStaticMethodsFromJavaClass(accessorName)
                    }
                } && (property is KMutableProperty<*> || !JvmAbi.isSetterName(function.name))
            }
        }) return false

    return !doesOverrideRenamedBuiltins(function)
}

private fun MemberContainer<*>.getDeclaredNonStaticMethodsFromJavaClass(name: String? = null): List<JavaKNamedFunction> {
    require(kmClass == null) { "Should be called only for Java classes: $this" }
    if (jClass.isAnnotation) return emptyList()
    return jClass.declaredMethods.mapNotNull { method ->
        if ((name != null && method.name != name) || Modifier.isStatic(method.modifiers) || method.isSynthetic) null
        else JavaKNamedFunction(this, method, NO_RECEIVER, KCallableOverriddenStorage.EMPTY)
    }
}

private fun MemberContainer<*>.getPropertiesFromSupertypes(name: String): List<KProperty1<*, *>> =
    supertypes.flatMap { supertype -> supertype.memberContainer?.memberProperties?.filter { it.name == name }.orEmpty() }

private val ReflectKFunction.jvmName: String
    get() = signature.substringBeforeLast('(')

private fun ReflectKFunction.doesOverrideBuiltinWithDifferentJvmName(jvmName: String): Boolean =
    this.jvmName == jvmName || overridden.any { it.doesOverrideBuiltinWithDifferentJvmName(jvmName) }

private fun MemberContainer<*>.doesOverrideRenamedBuiltins(function: JavaKNamedFunction): Boolean {
    val method = function.jMethod
    val builtinName = SpecialGenericSignatures.getBuiltinFunctionNamesByJvmName(Name.identifier(method.name)) ?: return false
    return obtainOverrideForBuiltinWithDifferentJvmName(method, builtinName.asString()) != null
}

private fun MemberContainer<*>.obtainOverrideForBuiltinWithDifferentJvmName(method: Method, kotlinName: String): JavaKNamedFunction? {
    val renamed = JavaKNamedFunction(this, method, NO_RECEIVER, KCallableOverriddenStorage.EMPTY, kotlinName)
    return renamed.takeIf { it.overridden.any { overridden -> overridden.doesOverrideBuiltinWithDifferentJvmName(method.name) } }
}

private fun MemberContainer<*>.addOverriddenSpecialMethods(name: String, result: MutableCollection<ReflectKCallable<*>>) {
    if (Name.identifier(name) !in SpecialGenericSignatures.ORIGINAL_SHORT_NAMES) return
    val jvmNamesFromSupertypes = getFunctionsFromSupertypes(name).mapTo(HashSet()) { it.jvmName }.apply { remove(name) }
    if (jvmNamesFromSupertypes.isEmpty()) return
    for (method in jClass.declaredMethods) {
        if (method.name !in jvmNamesFromSupertypes || Modifier.isStatic(method.modifiers) || method.isSynthetic) continue
        obtainOverrideForBuiltinWithDifferentJvmName(method, name)?.let(result::add)
    }
}

private fun MemberContainer<*>.getFunctionsFromSupertypes(name: String): List<ReflectKFunction> =
    supertypes.flatMap { supertype ->
        supertype.memberContainer?.getFakeOverrideMembersByName(name)?.values?.filterIsInstance<ReflectKFunction>().orEmpty()
    }

private fun doesClassOverrideProperty(
    property: KProperty1<*, *>,
    functions: (String) -> Collection<ReflectKFunction>,
): Boolean {
    // Java fields cannot be overridden.
    if (property is JavaFieldKProperty<*>) return false

    val getter = property.findGetterOverride(functions)
    val setter = property.findSetterOverride(functions)

    if (getter == null) return false
    if (property !is KMutableProperty<*>) return true

    return setter != null && setter.modality == getter.modality
}

private fun KProperty1<*, *>.findGetterOverride(functions: (String) -> Collection<ReflectKFunction>): ReflectKFunction? =
    findGetterByName(getBuiltinSpecialPropertyGetterName() ?: JvmAbi.getterName(name), functions)

private fun KProperty1<*, *>.getBuiltinSpecialPropertyGetterName(): String? {
    if (this !is KotlinKProperty<*>) return null
    if (Name.identifier(name) !in BuiltinSpecialProperties.SPECIAL_SHORT_NAMES) return null
    return signature.substringBeforeLast('(').takeIf { it != JvmAbi.getterName(name) }
}

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
internal fun MemberContainer<*>.getAdditionalFunctions(): List<ReflectKFunction> {
    if (!isMappedBuiltin || this == Any::class) return emptyList()
    val kmClass = kmClass ?: return emptyList()
    val isMutable = this is MutableCollectionKClass<*>

    val javaAnalogue = jClass.wrapperByPrimitive ?: jClass

    // Names of properties declared in this class's metadata, and JVM signatures of functions declared in this class's metadata.
    // For collection classes, these also include members of the whole Kotlin collection hierarchy, see `collectMembersOfCollectionHierarchy`.
    val propertyNames = kmClass.properties.mapTo(HashSet()) { it.name }
    val declaredJvmSignatures = kmClass.functions.mapTo(HashSet()) {
        it.computeJvmSignature(this).toString()
    }
    collectMembersOfCollectionHierarchy(kmClass, propertyNames, declaredJvmSignatures)

    // Property accessors must not be loaded as functions; the compiler filters them out because they override the corresponding
    // property accessors declared in this class. Unlike functions (handled below), reflection keeps properties and functions separate,
    // so they are not deduplicated against each other automatically.
    val getterLikeNames = HashSet<String>()   // matched against 0-arg methods, e.g. Enum.name()/ordinal() and Throwable.getMessage()
    val setterLikeNames = HashSet<String>()   // matched against 1-arg methods
    for (propertyName in propertyNames) {
        getterLikeNames += propertyName
        getterLikeNames += JvmAbi.getterName(propertyName)
        // Getters of some builtin properties have special JVM names, e.g. `keySet` for `Map.keys`.
        getBuiltinSpecialPropertyGetterName(propertyName, this)?.let(getterLikeNames::add)
        setterLikeNames += JvmAbi.setterName(propertyName)
    }

    return javaAnalogue.declaredMethods.mapNotNull { method ->
        if (Modifier.isStatic(method.modifiers) || method.isSynthetic) return@mapNotNull null
        if (!Modifier.isPublic(method.modifiers) && !Modifier.isProtected(method.modifiers)) return@mapNotNull null
        if (method.isAnnotationPresent(JavaLangDeprecated::class.java)) return@mapNotNull null

        // Methods which mutate the collection belong to the mutable collection class only, and all other methods belong to the read-only
        // class only (the mutable class inherits them). This mirrors `JvmBuiltInsCustomizer.isMutabilityViolation`.
        val isMutableMethod = SignatureBuildingComponents.signature(javaAnalogue.classId.internalName, method.jvmSignature) in
                JvmBuiltInsSignatures.MUTABLE_METHOD_SIGNATURES
        if (isMutableMethod != isMutable) return@mapNotNull null

        val parameterCount = method.parameterTypes.size
        if (parameterCount == 0 && method.name in getterLikeNames) return@mapNotNull null
        if (parameterCount == 1 && method.name in setterLikeNames) return@mapNotNull null

        // Skip a Java method if it corresponds to a function already present in the Kotlin class: either declared in its metadata, or
        // inherited from a supertype (e.g. `equals`/`hashCode`/`toString` from `kotlin.Any`, or `compareTo` from `Comparable`).
        // Otherwise the Java-based function, which has flexible types (`equals(Any!)` instead of `equals(Any?)`), would replace the
        // Kotlin one. This mirrors the `kotlinVersions` check in `JvmBuiltInsCustomizer.getAdditionalFunctions`.
        if (method.jvmSignature in declaredJvmSignatures) return@mapNotNull null

        when (method.getJdkMethodStatus(javaAnalogue)) {
            JdkMemberStatus.DROP -> return@mapNotNull null
            // Hidden-for-resolution members are still listed by reflection, except in final classes where the compiler drops them.
            JdkMemberStatus.HIDDEN -> if (isFinal) return@mapNotNull null
            JdkMemberStatus.VISIBLE, JdkMemberStatus.DEPRECATED_LIST_METHODS, JdkMemberStatus.NOT_CONSIDERED -> {}
        }

        val function = JavaKNamedFunction(this, method, NO_RECEIVER, KCallableOverriddenStorage.EMPTY)
        // Keep only those additional Java methods which override only other additional Java methods. This is necessary e.g. for
        // `String.chars` which overrides `CharSequence.chars`.
        if (function.overridden.any { it !is JavaKNamedFunction }) return@mapNotNull null

        function
    }
}

/**
 * Supertypes of the Java analogue class of this mapped built-in class, e.g. `java.lang.Iterable<E!>` for `kotlin.collections.Collection`
 * (whose Java analogue is `java.util.Collection`), converted from Java reflection in the same way as supertypes of any Java class.
 *
 * Additional functions (see `getAdditionalFunctions`) are enhanced in the same way as members of the Java analogue class would be: by the
 * signatures of the overridden functions found through these supertypes. Unlike in supertypes of the Kotlin class (`Iterable<E>`), type
 * arguments in Java supertypes are flexible, which affects the result, e.g. `java.util.Collection.spliterator()` overriding
 * `java.lang.Iterable.spliterator(): Spliterator<T>` is enhanced to `Spliterator<E!>`, not `Spliterator<E>`.
 */
internal val MemberContainer<*>.javaAnalogueSupertypes: List<KType>
    get() {
        val javaAnalogue = jClass.wrapperByPrimitive ?: jClass
        return listOfNotNull(javaAnalogue.genericSuperclass, *javaAnalogue.genericInterfaces).mapNotNull { superClass ->
            if (superClass == Any::class.java) return@mapNotNull null
            superClass.toKType(
                knownTypeParameters = emptyMap(), nullability = TypeNullability.NOT_NULL, howThisTypeIsUsed = TypeUsage.SUPERTYPE,
            )
        }
    }

// For a collection class, collects names of properties and JVM signatures of functions of the whole Kotlin collection hierarchy: for a
// mutable class (e.g. `MutableList`), all its supertypes (`List`, `MutableCollection`, `Collection`, ...); for a read-only class
// (e.g. `List`), its mutable counterpart and all its supertypes. Java methods of the Java analogue corresponding to these members must not
// be added as additional functions: they are either inherited by the Kotlin class from its supertypes (e.g. `size` and `remove` in
// `MutableList`), or belong to the mutable counterpart of the read-only class (e.g. `add` in `List`, see the `kotlinVersions` check in
// `JvmBuiltInsCustomizer.getAdditionalFunctions`). Does nothing if this class is not a collection class.
private fun MemberContainer<*>.collectMembersOfCollectionHierarchy(
    kmClass: KmClass,
    propertyNames: MutableSet<String>,
    functionJvmSignatures: MutableSet<String>,
) {
    val root = if (this is MutableCollectionKClass<*>) kmClass else (getMutableCollectionKClass(this)?.mutableKmClass ?: return)
    val visited = HashSet<String>()
    val queue = ArrayDeque<KmClass>().apply { add(root) }
    while (queue.isNotEmpty()) {
        val klass = queue.removeFirst()
        if (!visited.add(klass.name)) continue
        for (property in klass.properties) {
            propertyNames.add(property.name)
        }
        for (function in klass.functions) {
            val mapped = function.mapSignature(klass)
            val jvmName = getBuiltinSpecialFunctionJvmName(function.name, mapped.descriptor, this) ?: mapped.name
            functionJvmSignatures.add(JvmMethodSignature(jvmName, mapped.descriptor).toString())
        }
        for (supertype in klass.supertypes) {
            val superClassId = (supertype.classifier as? KmClassifier.Class)?.name?.toClassId() ?: continue
            readBuiltinClassMetadata(superClassId)?.let(queue::add)
        }
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

private fun MemberContainer<*>.addPropertyOverrideByMethod(
    propertiesFromSupertypes: List<KProperty1<*, *>>,
    result: MutableCollection<ReflectKCallable<*>>,
    handledProperties: MutableSet<KProperty1<*, *>>?,
    functions: (String) -> Collection<ReflectKFunction>
) {
    for (property in propertiesFromSupertypes) {
        val newProperty = createPropertyByMethods(property, functions)
        if (newProperty != null) {
            result.add(newProperty)
            handledProperties?.add(property)
            break
        }
    }
}

private fun MemberContainer<*>.createPropertyByMethods(
    overriddenProperty: KProperty1<*, *>,
    functions: (String) -> Collection<ReflectKFunction>,
): ReflectKProperty<*>? {
    if (!doesClassOverrideProperty(overriddenProperty, functions)) return null

    val getterMethod = overriddenProperty.findGetterOverride(functions)!!
    overriddenProperty as ReflectKProperty<*>
    return if (overriddenProperty is KMutableProperty<*>)
        JavaForKotlinOverrideKMutableProperty1<Any, Any>(
            this, rawBoundReceiver = NO_RECEIVER, KCallableOverriddenStorage.EMPTY, getterMethod,
            overriddenProperty.findSetterOverride(functions)!!, overriddenProperty,
        )
    else
        JavaForKotlinOverrideKProperty1<Any, Any>(
            this, rawBoundReceiver = NO_RECEIVER, KCallableOverriddenStorage.EMPTY, getterMethod, setterMethod = null, overriddenProperty,
        )
}
