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
import org.jetbrains.asm4.Type;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class  ParametersBuilder {

    private final List<ParameterInfo> params = new ArrayList<ParameterInfo>();
    private final List<CapturedParamInfo> capturedParams = new ArrayList<CapturedParamInfo>();

    private int nextIndex = 0;
    private int nextCaptured = 0;

    public static ParametersBuilder newBuilder() {
        return new ParametersBuilder();
    }

    public ParameterInfo addThis(Type type, boolean skipped) {
        ParameterInfo info = new ParameterInfo(type, skipped, nextIndex, -1);
        addParameter(info);
        return info;
    }

    public ParametersBuilder addNextParameter(Type type, boolean skipped, @Nullable ParameterInfo original) {
        addParameter(new ParameterInfo(type, skipped, nextIndex, original != null ? original.getIndex() : -1));
        return this;
    }

    public CapturedParamInfo addCapturedParam(
            @NotNull CapturedParamInfo param,
            @Nullable CapturedParamInfo originalField
    ) {
        CapturedParamInfo info = new CapturedParamInfo(param.desc, param.isSkipped, nextCaptured, originalField != null ? originalField.getIndex() : -1);
        info.setLambda(param.getLambda());
        return addCapturedParameter(info);
    }

    public CapturedParamInfo addCapturedParam(
            String fieldName,
            Type type,
            boolean skipped,
            @Nullable ParameterInfo original,
            CapturedParamOwner containingLambda
    ) {
        CapturedParamInfo info =
                new CapturedParamInfo(CapturedParamDesc.createDesc(containingLambda, fieldName, type), skipped, nextCaptured,
                                      original != null ? original.getIndex() : -1);
        if (original != null) {
            info.setLambda(original.getLambda());
        }
        return addCapturedParameter(info);
    }

    private void addParameter(ParameterInfo info) {
        params.add(info);
        nextIndex += info.getType().getSize();
    }

    private CapturedParamInfo addCapturedParameter(CapturedParamInfo info) {
        capturedParams.add(info);
        nextCaptured += info.getType().getSize();
        return info;
    }

    public List<ParameterInfo> build() {
        return Collections.unmodifiableList(params);
    }

    public List<CapturedParamInfo> buildCaptured() {
        return Collections.unmodifiableList(capturedParams);
    }

    public List<ParameterInfo> buildWithStubs() {
        return Parameters.addStubs(build());
    }

    public List<CapturedParamInfo> buildCapturedWithStubs() {
        return Parameters.shiftAndAddStubs(buildCaptured(), nextIndex);
    }

    public Parameters buildParameters() {
        return new Parameters(buildWithStubs(), buildCapturedWithStubs());
    }
}
