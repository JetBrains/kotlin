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

package org.jetbrains.kotlin.renderer;

import kotlin.Function1;
import kotlin.KotlinPackage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.types.JetType;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public class DescriptorRendererBuilder {
    private NameShortness nameShortness = NameShortness.SOURCE_CODE_QUALIFIED;
    private boolean withDefinedIn = true;
    private Set<DescriptorRenderer.Modifier> modifiers = EnumSet.allOf(DescriptorRenderer.Modifier.class);
    private boolean startFromName = false;
    private boolean debugMode = false;
    private boolean classWithPrimaryConstructor = false;
    private boolean verbose = false;
    private boolean unitReturnType = true;
    private boolean normalizedVisibilities = false;
    private boolean showInternalKeyword = true;
    private boolean prettyFunctionTypes = true;
    private boolean uninferredTypeParameterAsName = false;
    private boolean includePropertyConstant = false;
    private boolean includeSynthesizedParameterNames = true;
    private boolean withoutFunctionParameterNames = false;
    private boolean withoutTypeParameters = false;
    private boolean withoutSuperTypes = false;
    private Function1<JetType, JetType> typeNormalizer = new Function1<JetType, JetType>() {
        @Override
        public JetType invoke(JetType type) {
            return type;
        }
    };
    private boolean renderDefaultValues = true;
    private boolean flexibleTypesForCode = false;
    private boolean secondaryConstructorsAsPrimary = true;

    @NotNull
    private DescriptorRenderer.OverrideRenderingPolicy overrideRenderingPolicy = DescriptorRenderer.OverrideRenderingPolicy.RENDER_OPEN;
    @NotNull
    private DescriptorRenderer.ValueParametersHandler valueParametersHandler = new DescriptorRenderer.DefaultValueParameterHandler();
    @NotNull
    private DescriptorRenderer.TextFormat textFormat = DescriptorRenderer.TextFormat.PLAIN;
    @NotNull
    private Collection<FqName> excludedAnnotationClasses = Collections.emptyList();
    private boolean receiverAfterName = false;
    private boolean renderDefaultObjectName = false;

    public DescriptorRendererBuilder() {
    }

    @NotNull
    public DescriptorRendererBuilder setNameShortness(NameShortness shortness) {
        this.nameShortness = shortness;
        return this;
    }

    @NotNull
    public DescriptorRendererBuilder setWithDefinedIn(boolean withDefinedIn) {
        this.withDefinedIn = withDefinedIn;
        return this;
    }

    @NotNull
    public DescriptorRendererBuilder setModifiers(Set<DescriptorRenderer.Modifier> modifiers) {
        this.modifiers = modifiers;
        return this;
    }

    @NotNull
    public DescriptorRendererBuilder setModifiers(DescriptorRenderer.Modifier... modifiers) {
        return setModifiers(KotlinPackage.setOf(modifiers));
    }

    @NotNull
    public DescriptorRendererBuilder setStartFromName(boolean startFromName) {
        this.startFromName = startFromName;
        return this;
    }

    @NotNull
    public DescriptorRendererBuilder setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
        return this;
    }

    @NotNull
    public DescriptorRendererBuilder setClassWithPrimaryConstructor(boolean classWithPrimaryConstructor) {
        this.classWithPrimaryConstructor = classWithPrimaryConstructor;
        return this;
    }

    @NotNull
    public DescriptorRendererBuilder setVerbose(boolean verbose) {
        this.verbose = verbose;
        return this;
    }

    @NotNull
    public DescriptorRendererBuilder setUnitReturnType(boolean unitReturnType) {
        this.unitReturnType = unitReturnType;
        return this;
    }

    @NotNull
    public DescriptorRendererBuilder setNormalizedVisibilities(boolean normalizedVisibilities) {
        this.normalizedVisibilities = normalizedVisibilities;
        return this;
    }

    @NotNull
    public DescriptorRendererBuilder setShowInternalKeyword(boolean showInternalKeyword) {
        this.showInternalKeyword = showInternalKeyword;
        return this;
    }

    @NotNull
    public DescriptorRendererBuilder setOverrideRenderingPolicy(@NotNull DescriptorRenderer.OverrideRenderingPolicy overrideRenderingPolicy) {
        this.overrideRenderingPolicy = overrideRenderingPolicy;
        return this;
    }

    @NotNull
    public DescriptorRendererBuilder setValueParametersHandler(@NotNull DescriptorRenderer.ValueParametersHandler valueParametersHandler) {
        this.valueParametersHandler = valueParametersHandler;
        return this;
    }

    @NotNull
    public DescriptorRendererBuilder setTextFormat(@NotNull DescriptorRenderer.TextFormat textFormat) {
        this.textFormat = textFormat;
        return this;
    }

    @NotNull
    public DescriptorRendererBuilder setExcludedAnnotationClasses(@NotNull Collection<FqName> excludedAnnotationClasses) {
        this.excludedAnnotationClasses = excludedAnnotationClasses;
        return this;
    }

    @NotNull
    public DescriptorRendererBuilder setPrettyFunctionTypes(boolean prettyFunctionTypes) {
        this.prettyFunctionTypes = prettyFunctionTypes;
        return this;
    }

    @NotNull
    public DescriptorRendererBuilder setUninferredTypeParameterAsName(boolean uninferredTypeParameterAsName) {
        this.uninferredTypeParameterAsName = uninferredTypeParameterAsName;
        return this;
    }

    @NotNull
    public DescriptorRendererBuilder setIncludePropertyConstant(boolean includePropertyConstant) {
        this.includePropertyConstant = includePropertyConstant;
        return this;
    }

    @NotNull
    public DescriptorRendererBuilder setIncludeSynthesizedParameterNames(boolean includeSynthesizedParameterNames) {
        this.includeSynthesizedParameterNames = includeSynthesizedParameterNames;
        return this;
    }

    @NotNull
    public DescriptorRendererBuilder setWithoutTypeParameters(boolean withoutTypeParameters) {
        this.withoutTypeParameters = withoutTypeParameters;
        return this;
    }

    @NotNull
    public DescriptorRendererBuilder setWithoutFunctionParameterNames(boolean withoutFunctionParameterNames) {
        this.withoutFunctionParameterNames = withoutFunctionParameterNames;
        return this;
    }

    @NotNull
    public DescriptorRendererBuilder setReceiverAfterName(boolean receiverAfterName) {
        this.receiverAfterName = receiverAfterName;
        return this;
    }

    @NotNull
    public DescriptorRendererBuilder setRenderDefaultObjectName(boolean renderDefaultObjectName) {
        this.renderDefaultObjectName = renderDefaultObjectName;
        return this;
    }

    @NotNull
    public DescriptorRendererBuilder setWithoutSuperTypes(boolean withoutSuperTypes) {
        this.withoutSuperTypes = withoutSuperTypes;
        return this;
    }

    @NotNull
    public DescriptorRendererBuilder setRenderDefaultValues(boolean renderDefaultValues) {
        this.renderDefaultValues = renderDefaultValues;
        return this;
    }

    @NotNull
    public DescriptorRendererBuilder setTypeNormalizer(@NotNull Function1<JetType, JetType> typeNormalizer) {
        this.typeNormalizer = typeNormalizer;
        return this;
    }

    @NotNull
    public DescriptorRendererBuilder setFlexibleTypesForCode(boolean flexibleTypesForCode) {
        this.flexibleTypesForCode = flexibleTypesForCode;
        return this;
    }

    @NotNull
    public DescriptorRendererBuilder setSecondaryConstructorsAsPrimary(boolean secondaryConstructorsAsPrimary) {
        this.secondaryConstructorsAsPrimary = secondaryConstructorsAsPrimary;
        return this;
    }

    @NotNull
    public DescriptorRenderer build() {
        return new DescriptorRendererImpl(
                nameShortness, withDefinedIn, modifiers, startFromName, debugMode, classWithPrimaryConstructor, verbose, unitReturnType,
                normalizedVisibilities, showInternalKeyword, prettyFunctionTypes, uninferredTypeParameterAsName,
                overrideRenderingPolicy, valueParametersHandler, textFormat, excludedAnnotationClasses, includePropertyConstant,
                includeSynthesizedParameterNames, withoutFunctionParameterNames, withoutTypeParameters, receiverAfterName,
                renderDefaultObjectName, withoutSuperTypes, typeNormalizer, renderDefaultValues, flexibleTypesForCode,
                secondaryConstructorsAsPrimary);
    }

}
