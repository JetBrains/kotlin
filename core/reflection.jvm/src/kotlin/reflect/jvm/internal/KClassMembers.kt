/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal

import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.java.getPropertyNamesCandidatesByAccessorName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.utils.newHashMapWithExpectedSize
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

internal fun KClassImpl<*>.computeDeclaredMembers(): Collection<ReflectKCallable<*>> = buildList {
    val kClass = this@computeDeclaredMembers
    if (useK1Implementation || isComplicatedBuiltinSubclass || kmClass != null) {
        addAll(getDescriptorBasedMembers(memberScope, DECLARED))
        addAll(getDescriptorBasedMembers(staticScope, DECLARED))
    } else {
        getDeclaredNonStaticMethodsFromJavaClass().filterTo(this) { isVisibleAsFunctionInCurrentClass(it) }
        getDescriptorBasedMembers(memberScope, DECLARED).filterTo(this) { it is KProperty<*> }
        for (method in jClass.declaredMethods) {
            if (Modifier.isStatic(method.modifiers) && !method.isSynthetic) {
                add(JavaKNamedFunction(kClass, method, NO_RECEIVER, KCallableOverriddenStorage.EMPTY))
            }
        }

        for (field in jClass.declaredFields) {
            if (field.isEnumConstant) continue
            if (Modifier.isStatic(field.modifiers) && !field.isSynthetic) {
                if (Modifier.isFinal(field.modifiers)) {
                    add(JavaKProperty0<Any>(kClass, field, NO_RECEIVER, KCallableOverriddenStorage.EMPTY))
                } else {
                    add(JavaKMutableProperty0<Any>(kClass, field, NO_RECEIVER, KCallableOverriddenStorage.EMPTY))
                }
            }
        }

        if (jClass.isEnum) {
            @Suppress("UNCHECKED_CAST")
            add(JavaEnumEntriesKProperty(kClass as KClassImpl<out Enum<*>>))
        }
    }
}

internal fun KClassImpl<*>.computeAllMembers(): Collection<ReflectKCallable<*>> =
    if (!newFakeOverridesImplementation || useK1Implementation || isComplicatedBuiltinSubclass) {
        buildList {
            addAll(data.value.declaredMembers)
            addAll(getDescriptorBasedMembers(memberScope, INHERITED))
            addAll(getDescriptorBasedMembers(staticScope, INHERITED))
        }
    } else {
        val fakeOverrideMembers = data.value.fakeOverrideMembers
        val isKotlin = java.isKotlin
        val members = fakeOverrideMembers.filterNotTo(
            newHashMapWithExpectedSize(
                // We expect that all non-transitive operations below (like filtering out statics or adding privates)
                // do not change the final size of the collection significantly.
                // We expect the size to stay more or less the same.
                expectedSize = fakeOverrideMembers.size
            )
        ) { (_, member) ->
            // Kotlin classes never inherit static members (neither from Java, nor from Kotlin).
            (isKotlin && member.isStatic && member.overriddenStorage.isFakeOverride) ||
                    (member.isPackagePrivate && member.originalContainer.jClass.`package` != java.`package`)
        }
        members.values + data.value.declaredMembers.filter { isNonTransitiveMember(this, it) }
    }

private fun KClassImpl<*>.getDescriptorBasedMembers(
    scope: MemberScope, belonginess: MemberBelonginess,
): Collection<DescriptorKCallable<*>> {
    val visitor = object : CreateKCallableVisitor(this) {
        override fun visitConstructorDescriptor(descriptor: ConstructorDescriptor, data: Unit): DescriptorKCallable<*> =
            throw IllegalStateException("No constructors should appear here: $descriptor")
    }
    return scope.getContributedDescriptors().mapNotNull { descriptor ->
        if (descriptor is CallableMemberDescriptor &&
            descriptor.visibility != DescriptorVisibilities.INVISIBLE_FAKE &&
            belonginess.accept(descriptor)
        ) descriptor.accept(visitor, Unit) else null
    }.toList()
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

internal fun KClassImpl<*>.getDeclaredNonStaticMethodsFromJavaClass(): List<JavaKNamedFunction> {
    require(kmClass == null) { "Should be called only for Java classes: $this" }
    if (jClass.isAnnotation) return emptyList()
    return jClass.declaredMethods.mapNotNull { method ->
        if (Modifier.isStatic(method.modifiers) || method.isSynthetic) null
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
