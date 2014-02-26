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

import org.jetbrains.asm4.Type;

import java.util.List;
import java.util.Map;

public class ConstructorInvocation {

    private final String ownerInternalName;

    private final Map<Integer, LambdaInfo> lambdasToInline;

    private final boolean isSameModule;

    private Type newLambdaType;

    private String newConstructorDescriptor;

    private List<CapturedParamInfo> allRecapturedParameters;

    private Map<String, LambdaInfo> capturedLambdasToInline;

    ConstructorInvocation(String ownerInternalName, Map<Integer, LambdaInfo> lambdasToInline, boolean isSameModule) {
        this.ownerInternalName = ownerInternalName;
        this.lambdasToInline = lambdasToInline;
        this.isSameModule = isSameModule;
    }

    public String getOwnerInternalName() {
        return ownerInternalName;
    }

    public boolean shouldRegenerate() {
        return !lambdasToInline.isEmpty() || !isSameModule;
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

    public List<CapturedParamInfo> getAllRecapturedParameters() {
        return allRecapturedParameters;
    }

    public void setAllRecapturedParameters(List<CapturedParamInfo> allRecapturedParameters) {
        this.allRecapturedParameters = allRecapturedParameters;
    }

    public Map<String, LambdaInfo> getCapturedLambdasToInline() {
        return capturedLambdasToInline;
    }

    public void setCapturedLambdasToInline(Map<String, LambdaInfo> capturedLambdasToInline) {
        this.capturedLambdasToInline = capturedLambdasToInline;
    }
}
