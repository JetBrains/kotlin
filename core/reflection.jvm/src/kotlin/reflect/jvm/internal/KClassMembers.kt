/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal

import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.java.getPropertyNamesCandidatesByAccessorName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import java.lang.reflect.Modifier
import kotlin.jvm.internal.CallableReference.NO_RECEIVER
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.valueParameters
import kotlin.reflect.jvm.internal.MemberBelonginess.DECLARED
import kotlin.reflect.jvm.internal.MemberBelonginess.INHERITED
import kotlin.reflect.jvm.internal.types.areEqualKTypes

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
    if (useK1Implementation || isComplicatedBuiltinSubclass || kmClass != null) {
        addAll(getDescriptorBasedMembers(memberScope, DECLARED, name))
        addAll(getDescriptorBasedMembers(staticScope, DECLARED, name))
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
    if (useK1Implementation || isComplicatedBuiltinSubclass || kmClass != null) {
        getMemberNamesFromDescriptors()
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
