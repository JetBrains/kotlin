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

package org.jetbrains.kotlin.renderer

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.types.JetType
import java.util.Collections
import java.util.EnumSet

public class DescriptorRendererBuilder {
    private var nameShortness = NameShortness.SOURCE_CODE_QUALIFIED
    private var withDefinedIn = true
    private var modifiers: Set<DescriptorRenderer.Modifier> = EnumSet.allOf(javaClass<DescriptorRenderer.Modifier>())
    private var startFromName = false
    private var debugMode = false
    private var classWithPrimaryConstructor = false
    private var verbose = false
    private var unitReturnType = true
    private var normalizedVisibilities = false
    private var showInternalKeyword = true
    private var prettyFunctionTypes = true
    private var uninferredTypeParameterAsName = false
    private var includePropertyConstant = false
    private var withoutTypeParameters = false
    private var withoutSuperTypes = false
    private var typeNormalizer: Function1<JetType, JetType> = object : Function1<JetType, JetType> {
        override fun invoke(type: JetType): JetType {
            return type
        }
    }
    private var renderDefaultValues = true
    private var flexibleTypesForCode = false
    private var secondaryConstructorsAsPrimary = true
    private var overrideRenderingPolicy: DescriptorRenderer.OverrideRenderingPolicy = DescriptorRenderer.OverrideRenderingPolicy.RENDER_OPEN
    private var valueParametersHandler: DescriptorRenderer.ValueParametersHandler = DescriptorRenderer.DefaultValueParameterHandler()
    private var textFormat: DescriptorRenderer.TextFormat = DescriptorRenderer.TextFormat.PLAIN
    private var parameterNameRenderingPolicy: DescriptorRenderer.ParameterNameRenderingPolicy = DescriptorRenderer.ParameterNameRenderingPolicy.ALL
    private var excludedAnnotationClasses: Collection<FqName> = emptyList()
    private var receiverAfterName = false
    private var renderCompanionObjectName = false
    private var renderAccessors = false

    // See JvmAnnotationNames#ANNOTATIONS_COPIED_TO_TYPES
    private var excludedTypeAnnotationClasses: Collection<FqName> = setOf(
            FqName("org.jetbrains.annotations.ReadOnly"),
            FqName("org.jetbrains.annotations.Mutable"),
            FqName("org.jetbrains.annotations.NotNull"),
            FqName("org.jetbrains.annotations.Nullable"))

    public fun setNameShortness(shortness: NameShortness): DescriptorRendererBuilder {
        this.nameShortness = shortness
        return this
    }

    public fun setWithDefinedIn(withDefinedIn: Boolean): DescriptorRendererBuilder {
        this.withDefinedIn = withDefinedIn
        return this
    }

    public fun setModifiers(modifiers: Set<DescriptorRenderer.Modifier>): DescriptorRendererBuilder {
        this.modifiers = modifiers
        return this
    }

    public fun setModifiers(vararg modifiers: DescriptorRenderer.Modifier): DescriptorRendererBuilder {
        return setModifiers(setOf(*modifiers))
    }

    public fun setStartFromName(startFromName: Boolean): DescriptorRendererBuilder {
        this.startFromName = startFromName
        return this
    }

    public fun setDebugMode(debugMode: Boolean): DescriptorRendererBuilder {
        this.debugMode = debugMode
        return this
    }

    public fun setClassWithPrimaryConstructor(classWithPrimaryConstructor: Boolean): DescriptorRendererBuilder {
        this.classWithPrimaryConstructor = classWithPrimaryConstructor
        return this
    }

    public fun setVerbose(verbose: Boolean): DescriptorRendererBuilder {
        this.verbose = verbose
        return this
    }

    public fun setUnitReturnType(unitReturnType: Boolean): DescriptorRendererBuilder {
        this.unitReturnType = unitReturnType
        return this
    }

    public fun setNormalizedVisibilities(normalizedVisibilities: Boolean): DescriptorRendererBuilder {
        this.normalizedVisibilities = normalizedVisibilities
        return this
    }

    public fun setShowInternalKeyword(showInternalKeyword: Boolean): DescriptorRendererBuilder {
        this.showInternalKeyword = showInternalKeyword
        return this
    }

    public fun setOverrideRenderingPolicy(overrideRenderingPolicy: DescriptorRenderer.OverrideRenderingPolicy): DescriptorRendererBuilder {
        this.overrideRenderingPolicy = overrideRenderingPolicy
        return this
    }

    public fun setValueParametersHandler(valueParametersHandler: DescriptorRenderer.ValueParametersHandler): DescriptorRendererBuilder {
        this.valueParametersHandler = valueParametersHandler
        return this
    }

    public fun setTextFormat(textFormat: DescriptorRenderer.TextFormat): DescriptorRendererBuilder {
        this.textFormat = textFormat
        return this
    }

    public fun setExcludedAnnotationClasses(excludedAnnotationClasses: Collection<FqName>): DescriptorRendererBuilder {
        this.excludedAnnotationClasses = excludedAnnotationClasses
        return this
    }

    public fun setExcludedTypeAnnotationClasses(excludedTypeAnnotationClasses: Collection<FqName>): DescriptorRendererBuilder {
        this.excludedTypeAnnotationClasses = excludedTypeAnnotationClasses
        return this
    }

    public fun setPrettyFunctionTypes(prettyFunctionTypes: Boolean): DescriptorRendererBuilder {
        this.prettyFunctionTypes = prettyFunctionTypes
        return this
    }

    public fun setUninferredTypeParameterAsName(uninferredTypeParameterAsName: Boolean): DescriptorRendererBuilder {
        this.uninferredTypeParameterAsName = uninferredTypeParameterAsName
        return this
    }

    public fun setIncludePropertyConstant(includePropertyConstant: Boolean): DescriptorRendererBuilder {
        this.includePropertyConstant = includePropertyConstant
        return this
    }

    public fun setParameterNameRenderingPolicy(parameterNameRenderingPolicy: DescriptorRenderer.ParameterNameRenderingPolicy): DescriptorRendererBuilder {
        this.parameterNameRenderingPolicy = parameterNameRenderingPolicy
        return this
    }

    public fun setWithoutTypeParameters(withoutTypeParameters: Boolean): DescriptorRendererBuilder {
        this.withoutTypeParameters = withoutTypeParameters
        return this
    }

    public fun setReceiverAfterName(receiverAfterName: Boolean): DescriptorRendererBuilder {
        this.receiverAfterName = receiverAfterName
        return this
    }

    public fun setRenderCompanionObjectName(renderCompanionObjectName: Boolean): DescriptorRendererBuilder {
        this.renderCompanionObjectName = renderCompanionObjectName
        return this
    }

    public fun setWithoutSuperTypes(withoutSuperTypes: Boolean): DescriptorRendererBuilder {
        this.withoutSuperTypes = withoutSuperTypes
        return this
    }

    public fun setRenderDefaultValues(renderDefaultValues: Boolean): DescriptorRendererBuilder {
        this.renderDefaultValues = renderDefaultValues
        return this
    }

    public fun setTypeNormalizer(typeNormalizer: Function1<JetType, JetType>): DescriptorRendererBuilder {
        this.typeNormalizer = typeNormalizer
        return this
    }

    public fun setFlexibleTypesForCode(flexibleTypesForCode: Boolean): DescriptorRendererBuilder {
        this.flexibleTypesForCode = flexibleTypesForCode
        return this
    }

    public fun setSecondaryConstructorsAsPrimary(secondaryConstructorsAsPrimary: Boolean): DescriptorRendererBuilder {
        this.secondaryConstructorsAsPrimary = secondaryConstructorsAsPrimary
        return this
    }

    public fun setRenderAccessors(renderAccessors: Boolean): DescriptorRendererBuilder {
        this.renderAccessors = renderAccessors
        return this
    }

    public fun build(): DescriptorRenderer {
        return DescriptorRendererImpl(nameShortness, withDefinedIn, modifiers, startFromName, debugMode, classWithPrimaryConstructor, verbose, unitReturnType, normalizedVisibilities, showInternalKeyword, prettyFunctionTypes, uninferredTypeParameterAsName, overrideRenderingPolicy, valueParametersHandler, textFormat, excludedAnnotationClasses, excludedTypeAnnotationClasses, includePropertyConstant, parameterNameRenderingPolicy, withoutTypeParameters, receiverAfterName, renderCompanionObjectName, withoutSuperTypes, typeNormalizer, renderDefaultValues, flexibleTypesForCode, secondaryConstructorsAsPrimary, renderAccessors)
    }

}
