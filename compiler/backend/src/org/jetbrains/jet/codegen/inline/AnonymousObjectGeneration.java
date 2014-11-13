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

package org.jetbrains.jet.codegen.inline;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.org.objectweb.asm.Type;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnonymousObjectGeneration {

    private final String ownerInternalName;

    private final String constructorDesc;

    private final Map<Integer, LambdaInfo> lambdasToInline;

    private final boolean isSameModule;

    private Type newLambdaType;

    private String newConstructorDescriptor;

    private List<CapturedParamDesc> allRecapturedParameters;

    private Map<String, LambdaInfo> capturedLambdasToInline;

    private final boolean capturedOuterRegenerated;
    private final boolean needReification;
    private final boolean alreadyRegenerated;
    private final boolean isStaticOrigin;

    AnonymousObjectGeneration(
            @NotNull String ownerInternalName,
            boolean needReification,
            boolean isSameModule,
            @NotNull Map<Integer, LambdaInfo> lambdasToInline,
            boolean capturedOuterRegenerated,
            boolean alreadyRegenerated,
            @Nullable String constructorDesc,
            boolean isStaticOrigin
    ) {
        this.ownerInternalName = ownerInternalName;
        this.constructorDesc = constructorDesc;
        this.lambdasToInline = lambdasToInline;
        this.isSameModule = isSameModule;
        this.capturedOuterRegenerated = capturedOuterRegenerated;
        this.needReification = needReification;
        this.alreadyRegenerated = alreadyRegenerated;
        this.isStaticOrigin = isStaticOrigin;
    }

    public AnonymousObjectGeneration(
            @NotNull String ownerInternalName, boolean isSameModule, boolean needReification,
            boolean alreadyRegenerated,
            boolean isStaticOrigin
    ) {
        this(
                ownerInternalName, isSameModule, needReification,
                new HashMap<Integer, LambdaInfo>(), false, alreadyRegenerated, null, isStaticOrigin
        );
    }

    public String getOwnerInternalName() {
        return ownerInternalName;
    }

    public boolean shouldRegenerate() {
        return !alreadyRegenerated && (
                !lambdasToInline.isEmpty() || !isSameModule || capturedOuterRegenerated || needReification
        );
    }

    public Map<Integer, LambdaInfo> getLambdasToInline() {
        return lambdasToInline;
    }

    public Type getNewLambdaType() {
        return newLambdaType;
    }

    public void setNewLambdaType(Type newLambdaType) {
        this.newLambdaType = newLambdaType;
    }

    public String getNewConstructorDescriptor() {
        return newConstructorDescriptor;
    }

    public void setNewConstructorDescriptor(String newConstructorDescriptor) {
        this.newConstructorDescriptor = newConstructorDescriptor;
    }

    public List<CapturedParamDesc> getAllRecapturedParameters() {
        return allRecapturedParameters;
    }

    public void setAllRecapturedParameters(List<CapturedParamDesc> allRecapturedParameters) {
        this.allRecapturedParameters = allRecapturedParameters;
    }

    public Map<String, LambdaInfo> getCapturedLambdasToInline() {
        return capturedLambdasToInline;
    }

    public void setCapturedLambdasToInline(Map<String, LambdaInfo> capturedLambdasToInline) {
        this.capturedLambdasToInline = capturedLambdasToInline;
    }

    @Nullable
    public String getConstructorDesc() {
        return constructorDesc;
    }

    public boolean isStaticOrigin() {
        return isStaticOrigin;
    }
}
