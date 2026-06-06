/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal

import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.java.getPropertyNamesCandidatesByAccessorName
import org.jetbrains.kotlin.name.Name
import java.lang.reflect.Modifier
import kotlin.jvm.internal.CallableReference.NO_RECEIVER
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty1
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.valueParameters
import kotlin.reflect.jvm.internal.types.areEqualKTypes

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
