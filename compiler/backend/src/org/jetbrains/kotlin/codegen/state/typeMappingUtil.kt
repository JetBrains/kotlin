/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:JvmName("TypeMappingUtil")

package org.jetbrains.kotlin.codegen.state

import org.jetbrains.kotlin.builtins.StandardNames.FqNames
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.load.kotlin.TypeMappingMode
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.firstOverridden
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull
import org.jetbrains.kotlin.resolve.descriptorUtil.parentsWithSelf
import org.jetbrains.kotlin.resolve.descriptorUtil.propertyIfAccessor
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeSystemCommonBackendContext
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.checker.SimpleClassicTypeSystemContext
import org.jetbrains.kotlin.types.checker.convertVariance
import org.jetbrains.kotlin.types.getEffectiveVariance
import org.jetbrains.kotlin.types.model.KotlinTypeMarker

fun TypeSystemCommonBackendContext.isMostPreciseContravariantArgument(type: KotlinTypeMarker): Boolean =
    type.typeConstructor().isAnyConstructor()

fun TypeSystemCommonBackendContext.isMostPreciseCovariantArgument(type: KotlinTypeMarker): Boolean =
    !canHaveSubtypesIgnoringNullability(type)

private fun TypeSystemCommonBackendContext.canHaveSubtypesIgnoringNullability(kotlinType: KotlinTypeMarker): Boolean {
    val constructor = kotlinType.typeConstructor()

    if (!constructor.isClassTypeConstructor() || !constructor.isFinalClassOrEnumEntryOrAnnotationClassConstructor()) return true

    for (i in 0 until constructor.parametersCount()) {
        val parameter = constructor.getParameter(i)
        val argument = kotlinType.getArgument(i)
        if (argument.isStarProjection()) return true

        val projectionKind = argument.getVariance().convertVariance()
        val type = argument.getType()

        val effectiveVariance = getEffectiveVariance(parameter.getVariance().convertVariance(), projectionKind)
        if (effectiveVariance == Variance.OUT_VARIANCE && !isMostPreciseCovariantArgument(type)) return true
        if (effectiveVariance == Variance.IN_VARIANCE && !isMostPreciseContravariantArgument(type)) return true
    }

    return false
}

val CallableDescriptor?.isMethodWithDeclarationSiteWildcards: Boolean
    get() {
        if (this !is CallableMemberDescriptor) return false
        return original.firstOverridden(useOriginal = true) {
            it.propertyIfAccessor.fqNameOrNull().isMethodWithDeclarationSiteWildcardsFqName
        } != null
    }

val FqName?.isMethodWithDeclarationSiteWildcardsFqName: Boolean
    get() = this in METHODS_WITH_DECLARATION_SITE_WILDCARDS

private fun FqName.child(name: String): FqName = child(Name.identifier(name))
private val METHODS_WITH_DECLARATION_SITE_WILDCARDS = setOf(
    FqNames.mutableCollection.child("addAll"),
    FqNames.mutableList.child("addAll"),
    FqNames.mutableMap.child("putAll")
)

fun TypeMappingMode.updateArgumentModeFromAnnotations(
    type: KotlinTypeMarker, typeSystem: TypeSystemCommonBackendContext
): TypeMappingMode {
    type.suppressWildcardsMode(typeSystem)?.let {
        return TypeMappingMode.createWithConstantDeclarationSiteWildcardsMode(
                skipDeclarationSiteWildcards = it,
                isForAnnotationParameter = isForAnnotationParameter,
                needInlineClassWrapping = needInlineClassWrapping,
                mapTypeAliases = mapTypeAliases
        )
    }

    if (with(typeSystem) { type.hasAnnotation(JVM_WILDCARD_ANNOTATION_FQ_NAME) }) {
        return TypeMappingMode.createWithConstantDeclarationSiteWildcardsMode(
            skipDeclarationSiteWildcards = false,
            isForAnnotationParameter = isForAnnotationParameter,
            fallbackMode = this,
            needInlineClassWrapping = needInlineClassWrapping,
            mapTypeAliases = mapTypeAliases
        )
    }

    return this
}

internal fun extractTypeMappingModeFromAnnotation(
    callableDescriptor: CallableDescriptor?,
    outerType: KotlinType,
    isForAnnotationParameter: Boolean,
    mapTypeAliases: Boolean
): TypeMappingMode? =
    SimpleClassicTypeSystemContext.extractTypeMappingModeFromAnnotation(
        callableDescriptor?.suppressWildcardsMode(), outerType, isForAnnotationParameter, mapTypeAliases
    )

fun TypeSystemCommonBackendContext.extractTypeMappingModeFromAnnotation(
    callableSuppressWildcardsMode: Boolean?,
    outerType: KotlinTypeMarker,
    isForAnnotationParameter: Boolean,
    mapTypeAliases: Boolean
): TypeMappingMode? {
    val suppressWildcards =
        outerType.suppressWildcardsMode(this) ?: callableSuppressWildcardsMode ?: return null

    if (outerType.argumentsCount() == 0) return TypeMappingMode.DEFAULT

    return TypeMappingMode.createWithConstantDeclarationSiteWildcardsMode(
        skipDeclarationSiteWildcards = suppressWildcards,
        isForAnnotationParameter = isForAnnotationParameter,
        needInlineClassWrapping = !outerType.typeConstructor().isInlineClass(),
        mapTypeAliases = mapTypeAliases
    )
}

private fun DeclarationDescriptor.suppressWildcardsMode(): Boolean? =
    parentsWithSelf.mapNotNull {
        it.annotations.findAnnotation(JVM_SUPPRESS_WILDCARDS_ANNOTATION_FQ_NAME)
    }.firstOrNull()?.suppressWildcardsMode()

private fun KotlinTypeMarker.suppressWildcardsMode(typeSystem: TypeSystemCommonBackendContext): Boolean? =
    with(typeSystem) {
        if (hasAnnotation(JVM_SUPPRESS_WILDCARDS_ANNOTATION_FQ_NAME))
            getAnnotationFirstArgumentValue(JVM_SUPPRESS_WILDCARDS_ANNOTATION_FQ_NAME) as? Boolean ?: true
        else null
    }

private fun AnnotationDescriptor.suppressWildcardsMode(): Boolean? =
    allValueArguments.values.firstOrNull()?.value as? Boolean ?: true

val JVM_SUPPRESS_WILDCARDS_ANNOTATION_FQ_NAME = FqName("kotlin.jvm.JvmSuppressWildcards")
val JVM_WILDCARD_ANNOTATION_FQ_NAME = FqName("kotlin.jvm.JvmWildcard")
