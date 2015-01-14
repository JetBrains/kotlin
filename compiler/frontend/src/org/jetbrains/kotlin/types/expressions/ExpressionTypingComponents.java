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

package org.jetbrains.kotlin.types.expressions;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.context.GlobalContext;
import org.jetbrains.kotlin.platform.PlatformToKotlinClassMap;
import org.jetbrains.kotlin.resolve.AdditionalCheckerProvider;
import org.jetbrains.kotlin.resolve.calls.CallResolver;
import org.jetbrains.kotlin.types.DynamicTypesSettings;
import org.jetbrains.kotlin.types.reflect.ReflectionTypes;

import javax.inject.Inject;

public class ExpressionTypingComponents {
    /*package*/ GlobalContext globalContext;
    /*package*/ ExpressionTypingServices expressionTypingServices;
    /*package*/ CallResolver callResolver;
    /*package*/ PlatformToKotlinClassMap platformToKotlinClassMap;
    /*package*/ ExpressionTypingUtils expressionTypingUtils;
    /*package*/ ControlStructureTypingUtils controlStructureTypingUtils;
    /*package*/ ForLoopConventionsChecker forLoopConventionsChecker;
    /*package*/ ReflectionTypes reflectionTypes;
    /*package*/ AdditionalCheckerProvider additionalCheckerProvider;
    /*package*/ DynamicTypesSettings dynamicTypesSettings;
    /*package*/ KotlinBuiltIns builtIns;
    /*package*/ LocalClassifierAnalyzer localClassifierAnalyzer;

    @Inject
    public void setGlobalContext(@NotNull GlobalContext globalContext) {
        this.globalContext = globalContext;
    }

    @Inject
    public void setExpressionTypingServices(@NotNull ExpressionTypingServices expressionTypingServices) {
        this.expressionTypingServices = expressionTypingServices;
    }

    @Inject
    public void setCallResolver(@NotNull CallResolver callResolver) {
        this.callResolver = callResolver;
    }

    @Inject
    public void setPlatformToKotlinClassMap(@NotNull PlatformToKotlinClassMap platformToKotlinClassMap) {
        this.platformToKotlinClassMap = platformToKotlinClassMap;
    }

    @Inject
    public void setExpressionTypingUtils(@NotNull ExpressionTypingUtils expressionTypingUtils) {
        this.expressionTypingUtils = expressionTypingUtils;
    }

    @Inject
    public void setControlStructureTypingUtils(@NotNull ControlStructureTypingUtils controlStructureTypingUtils) {
        this.controlStructureTypingUtils = controlStructureTypingUtils;
    }

    @Inject
    public void setForLoopConventionsChecker(@NotNull ForLoopConventionsChecker forLoopConventionsChecker) {
        this.forLoopConventionsChecker = forLoopConventionsChecker;
    }

    @Inject
    public void setReflectionTypes(@NotNull ReflectionTypes reflectionTypes) {
        this.reflectionTypes = reflectionTypes;
    }

    @Inject
    public void setAdditionalCheckerProvider(AdditionalCheckerProvider additionalCheckerProvider) {
        this.additionalCheckerProvider = additionalCheckerProvider;
    }

    @Inject
    public void setDynamicTypesSettings(@NotNull DynamicTypesSettings dynamicTypesSettings) {
        this.dynamicTypesSettings = dynamicTypesSettings;
    }

    @Inject
    public void setBuiltIns(@NotNull KotlinBuiltIns builtIns) {
        this.builtIns = builtIns;
    }

    @Inject
    public void setLocalClassifierAnalyzer(@NotNull LocalClassifierAnalyzer localClassifierAnalyzer) {
        this.localClassifierAnalyzer = localClassifierAnalyzer;
    }

    @NotNull
    public ForLoopConventionsChecker getForLoopConventionsChecker() {
        return forLoopConventionsChecker;
    }
}
