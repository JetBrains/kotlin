/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal

import org.jetbrains.kotlin.descriptors.runtime.structure.classId
import org.jetbrains.kotlin.descriptors.runtime.structure.wrapperByPrimitive
import org.jetbrains.kotlin.load.java.BuiltinSpecialProperties
import org.jetbrains.kotlin.load.java.SpecialGenericSignatures
import org.jetbrains.kotlin.load.kotlin.SignatureBuildingComponents
import org.jetbrains.kotlin.load.kotlin.internalName
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

// This is the descriptor-less counterpart of `specialBuiltinMembers.kt` and `ClassicBuiltinSpecialProperties.kt` in
// `core/descriptors.jvm`. It only ports the logic needed by kotlin-reflect to compute JVM signatures of declarations loaded from builtins
// metadata.

/**
 * If [propertyName] in [container] is a builtin property with a special JVM getter name (e.g. `name` for `kotlin.Enum.name`, `keySet` for
 * `kotlin.collections.Map.keys`), returns that name. Returns `null` for ordinary properties.
 */
internal fun getBuiltinSpecialPropertyGetterName(propertyName: String, container: KDeclarationContainerImpl): String? {
    if (container !is MemberContainer<*>) return null
    val fqName = getOverriddenBuiltinPropertyWithDifferentJvmName(propertyName, container)?.second ?: return null
    return BuiltinSpecialProperties.PROPERTY_FQ_NAME_TO_JVM_GETTER_NAME_MAP[fqName]?.asString()
}

/**
 * If [propertyName] in [container] is a builtin property with a special JVM getter name, or overrides such property, returns the builtin
 * class where that property is declared, and the fully qualified name of the property there.
 */
internal fun getOverriddenBuiltinPropertyWithDifferentJvmName(
    propertyName: String, container: MemberContainer<*>,
): Pair<MemberContainer<*>, FqName>? {
    if (Name.identifier(propertyName) !in BuiltinSpecialProperties.SPECIAL_SHORT_NAMES) return null
    return container.findBuiltinSpecialProperty(propertyName, hashSetOf())
}

private fun MemberContainer<*>.findBuiltinSpecialProperty(
    propertyName: String,
    visited: MutableSet<MemberContainer<*>>,
): Pair<MemberContainer<*>, FqName>? {
    if (!visited.add(this)) return null
    val fqName = classId.asSingleFqName().child(Name.identifier(propertyName))
    if (fqName in BuiltinSpecialProperties.SPECIAL_FQ_NAMES) return this to fqName
    for (supertype in supertypes) {
        (supertype.classifier as? MemberContainer<*>)?.findBuiltinSpecialProperty(propertyName, visited)?.let { return it }
    }
    return null
}

/**
 * If this function is a builtin function whose JVM name differs from its Kotlin name (e.g. `kotlin.Number.toInt`), or overrides such
 * function, returns that builtin function.
 */
internal fun ReflectKFunction.getOverriddenBuiltinWithDifferentJvmName(): ReflectKFunction? {
    if (Name.identifier(name) !in SpecialGenericSignatures.ORIGINAL_SHORT_NAMES) return null
    return firstOverridden(hashSetOf()) { it.isBuiltinFunctionWithDifferentNameInJvm() }
}

internal fun ReflectKFunction.doesOverrideBuiltinWithDifferentJvmName(): Boolean =
    getOverriddenBuiltinWithDifferentJvmName() != null

// See `BuiltinMethodsWithDifferentJvmName.isBuiltinFunctionWithDifferentNameInJvm` in the compiler.
private fun ReflectKFunction.isBuiltinFunctionWithDifferentNameInJvm(): Boolean =
    this is KotlinKNamedFunction && !overriddenStorage.isFakeOverride &&
            (container as? MemberContainer<*>)?.isMappedBuiltin == true && signature.substringBefore('(') != name

private fun ReflectKFunction.firstOverridden(
    visited: MutableSet<ReflectKFunction>,
    predicate: (ReflectKFunction) -> Boolean,
): ReflectKFunction? {
    if (!visited.add(this)) return null
    if (predicate(this)) return this
    for (overridden in overridden) {
        overridden.firstOverridden(visited, predicate)?.let { return it }
    }
    return null
}

/**
 * If [function] is a builtin function whose JVM name differs from its Kotlin name, returns that JVM name.
 */
internal fun getJvmMethodNameIfSpecial(function: ReflectKFunction): String? =
    function.getOverriddenBuiltinWithDifferentJvmName()?.signature?.substringBefore('(')

/**
 * Checks whether this Java class has a Kotlin (non-builtin) superclass which is a subclass of the builtin class containing
 * [specialCallable]. In this case, special bridges are already generated in that Kotlin superclass, and the Java class can only override
 * the method with the Kotlin name.
 */
internal fun MemberContainer<*>.hasRealKotlinSuperClassWithOverrideOf(specialCallable: ReflectKCallable<*>): Boolean =
    hasRealKotlinSuperClassWithOverrideOf(specialCallable.container as MemberContainer<*>)

internal fun MemberContainer<*>.hasRealKotlinSuperClassWithOverrideOf(builtinContainer: MemberContainer<*>): Boolean {
    var superClass = jClass.superclass
    while (superClass != null) {
        if (superClass.isKotlinClassOrPackage && builtinContainer.jClass.isAssignableFrom(superClass)) return true
        superClass = superClass.superclass
    }
    return false
}

/**
 * If the function [functionName] with the JVM [descriptor] in [container] is a builtin function whose JVM name differs from its Kotlin
 * name (e.g. `charAt` for `kotlin.CharSequence.get`, `intValue` for `kotlin.Number.toInt`, `remove` for `kotlin.collections.MutableList.removeAt`),
 * returns that JVM name. Returns `null` for ordinary functions.
 */
internal fun getBuiltinSpecialFunctionJvmName(functionName: String, descriptor: String, container: KDeclarationContainerImpl): String? {
    if (Name.identifier(functionName) !in SpecialGenericSignatures.ORIGINAL_SHORT_NAMES) return null
    val klass = container as? MemberContainer<*> ?: return null
    if (!klass.isMappedBuiltin) return null
    return klass.findBuiltinSpecialFunctionJvmName(functionName + descriptor)?.asString()
}

private fun MemberContainer<*>.findBuiltinSpecialFunctionJvmName(nameAndDescriptor: String): Name? {
    if (jClass.isArray) return null
    val javaAnalogue = jClass.wrapperByPrimitive ?: jClass
    val signature = SignatureBuildingComponents.signature(javaAnalogue.classId.internalName, nameAndDescriptor)
    SpecialGenericSignatures.SIGNATURE_TO_JVM_REPRESENTATION_NAME[signature]?.let { return it }
    for (supertype in supertypes) {
        val superclass = supertype.memberContainer ?: continue
        if (!superclass.isMappedBuiltin) continue
        superclass.findBuiltinSpecialFunctionJvmName(nameAndDescriptor)?.let { return it }
    }
    return null
}
