/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.renderer;

import com.google.common.collect.ImmutableSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.resolve.name.FqName;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

public class DescriptorRendererBuilder {
    private boolean shortNames = false;
    private boolean withDefinedIn = true;
    private Set<DescriptorRenderer.Modifier> modifiers = ImmutableSet.copyOf(DescriptorRenderer.Modifier.values());
    private boolean startFromName = false;
    private boolean debugMode = false;
    private boolean classWithPrimaryConstructor = false;
    private boolean verbose = false;
    private boolean unitReturnType = true;
    private boolean normalizedVisibilities = false;
    private boolean showInternalKeyword = true;
    @NotNull
    private DescriptorRenderer.OverrideRenderingPolicy overrideRenderingPolicy = DescriptorRenderer.OverrideRenderingPolicy.RENDER_OPEN;
    @NotNull
    private DescriptorRenderer.ValueParametersHandler valueParametersHandler = new DescriptorRenderer.DefaultValueParameterHandler();
    @NotNull
    private DescriptorRenderer.TextFormat textFormat = DescriptorRenderer.TextFormat.PLAIN;
    @NotNull
    private Collection<FqName> excludedAnnotationClasses = Collections.emptyList();

    public DescriptorRendererBuilder() {
    }

    public DescriptorRendererBuilder setShortNames(boolean shortNames) {
        this.shortNames = shortNames;
        return this;
    }

    public DescriptorRendererBuilder setWithDefinedIn(boolean withDefinedIn) {
        this.withDefinedIn = withDefinedIn;
        return this;
    }

    public DescriptorRendererBuilder setModifiers(Set<DescriptorRenderer.Modifier> modifiers) {
        this.modifiers = modifiers;
        return this;
    }

    public DescriptorRendererBuilder setModifiers(DescriptorRenderer.Modifier... modifiers) {
        return setModifiers(ImmutableSet.copyOf(modifiers));
    }

    public DescriptorRendererBuilder setStartFromName(boolean startFromName) {
        this.startFromName = startFromName;
        return this;
    }

    public DescriptorRendererBuilder setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
        return this;
    }

    public DescriptorRendererBuilder setClassWithPrimaryConstructor(boolean classWithPrimaryConstructor) {
        this.classWithPrimaryConstructor = classWithPrimaryConstructor;
        return this;
    }

    public DescriptorRendererBuilder setVerbose(boolean verbose) {
        this.verbose = verbose;
        return this;
    }

    public DescriptorRendererBuilder setUnitReturnType(boolean unitReturnType) {
        this.unitReturnType = unitReturnType;
        return this;
    }

    public DescriptorRendererBuilder setNormalizedVisibilities(boolean normalizedVisibilities) {
        this.normalizedVisibilities = normalizedVisibilities;
        return this;
    }

    public DescriptorRendererBuilder setShowInternalKeyword(boolean showInternalKeyword) {
        this.showInternalKeyword = showInternalKeyword;
        return this;
    }

    public DescriptorRendererBuilder setOverrideRenderingPolicy(DescriptorRenderer.OverrideRenderingPolicy overrideRenderingPolicy) {
        this.overrideRenderingPolicy = overrideRenderingPolicy;
        return this;
    }

    public DescriptorRendererBuilder setValueParametersHandler(@NotNull DescriptorRenderer.ValueParametersHandler valueParametersHandler) {
        this.valueParametersHandler = valueParametersHandler;
        return this;
    }

    public DescriptorRendererBuilder setTextFormat(@NotNull DescriptorRenderer.TextFormat textFormat) {
        this.textFormat = textFormat;
        return this;
    }

    public DescriptorRendererBuilder setExcludedAnnotationClasses(@NotNull Collection<FqName> excludedAnnotationClasses) {
        this.excludedAnnotationClasses = excludedAnnotationClasses;
        return this;
    }

    public DescriptorRenderer build() {
        return new DescriptorRendererImpl(shortNames, withDefinedIn, modifiers, startFromName, debugMode, classWithPrimaryConstructor,
                                          verbose, unitReturnType, normalizedVisibilities, showInternalKeyword,
                                          overrideRenderingPolicy, valueParametersHandler, textFormat, excludedAnnotationClasses);
    }

}
