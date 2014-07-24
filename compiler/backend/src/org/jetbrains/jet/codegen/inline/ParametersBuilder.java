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
import org.jetbrains.jet.codegen.FrameMap;
import org.jetbrains.jet.codegen.StackValue;
import org.jetbrains.org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class  ParametersBuilder {

    private final List<ParameterInfo> params = new ArrayList<ParameterInfo>();
    private final List<CapturedParamInfo> capturedParams = new ArrayList<CapturedParamInfo>();

    private int nextIndex = 0;
    private int nextCaptured = 0;

    @NotNull
    public static ParametersBuilder newBuilder() {
        return new ParametersBuilder();
    }

    @NotNull
    public ParameterInfo addThis(@NotNull Type type, boolean skipped) {
        ParameterInfo info = new ParameterInfo(type, skipped, nextIndex, -1);
        addParameter(info);
        return info;
    }

    @NotNull
    public ParameterInfo addNextParameter(@NotNull Type type, boolean skipped, @Nullable StackValue remapValue) {
        return addParameter(new ParameterInfo(type, skipped, nextIndex, remapValue));
    }

    @NotNull
    public CapturedParamInfo addCapturedParam(
            @NotNull CapturedParamInfo original,
            @NotNull String newFieldName
    ) {
        CapturedParamInfo info = new CapturedParamInfo(original.desc, newFieldName, original.isSkipped, nextCaptured, original.getIndex());
        info.setLambda(original.getLambda());
        return addCapturedParameter(info);
    }

    @NotNull
    public CapturedParamInfo addCapturedParam(
            @NotNull CapturedParamDesc desc,
            @NotNull String newFieldName
    ) {
        CapturedParamInfo info = new CapturedParamInfo(desc, newFieldName, false, nextCaptured, null);
        return addCapturedParameter(info);
    }

    @NotNull
    public CapturedParamInfo addCapturedParamCopy(
            @NotNull CapturedParamInfo copyFrom
    ) {
        CapturedParamInfo info = copyFrom.newIndex(nextCaptured);
        return addCapturedParameter(info);
    }

    @NotNull
    public CapturedParamInfo addCapturedParam(
            @NotNull CapturedParamOwner containingLambda,
            @NotNull String fieldName,
            @NotNull Type type,
            boolean skipped,
            @Nullable ParameterInfo original
    ) {
        CapturedParamInfo info =
                new CapturedParamInfo(CapturedParamDesc.createDesc(containingLambda, fieldName, type), skipped, nextCaptured,
                                      original != null ? original.getIndex() : -1);
        if (original != null) {
            info.setLambda(original.getLambda());
        }
        return addCapturedParameter(info);
    }

    @NotNull
    private ParameterInfo addParameter(ParameterInfo info) {
        params.add(info);
        nextIndex += info.getType().getSize();
        return info;
    }

    @NotNull
    private CapturedParamInfo addCapturedParameter(CapturedParamInfo info) {
        capturedParams.add(info);
        nextCaptured += info.getType().getSize();
        return info;
    }

    @NotNull
    public List<ParameterInfo> listNotCaptured() {
        return Collections.unmodifiableList(params);
    }

    @NotNull
    public List<CapturedParamInfo> listCaptured() {
        return Collections.unmodifiableList(capturedParams);
    }

    @NotNull
    public List<ParameterInfo> listAllParams() {
        List<ParameterInfo> list = new ArrayList<ParameterInfo>(params);
        list.addAll(capturedParams);
        return list;
    }

    @NotNull
    private List<ParameterInfo> buildWithStubs() {
        return Parameters.addStubs(listNotCaptured());
    }

    private List<CapturedParamInfo> buildCapturedWithStubs() {
        return Parameters.shiftAndAddStubs(listCaptured(), nextIndex);
    }

    public Parameters buildParameters() {
        return new Parameters(buildWithStubs(), buildCapturedWithStubs());
    }

    public int getNextValueParameterIndex() {
        return nextIndex;
    }
}
