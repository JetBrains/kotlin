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

package org.jetbrains.kotlin.codegen.inline;

import com.google.common.collect.Iterables;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

//All parameters with gaps
public class Parameters implements Iterable<ParameterInfo> {
    private final List<ParameterInfo> real;
    private final List<CapturedParamInfo> captured;

    public Parameters(List<ParameterInfo> real, List<CapturedParamInfo> captured) {
        this.real = real;
        this.captured = captured;
    }

    public List<ParameterInfo> getReal() {
        return real;
    }

    public List<CapturedParamInfo> getCaptured() {
        return captured;
    }

    public int totalSize() {
        return real.size() + captured.size();
    }

    public ParameterInfo get(int index) {
        if (index < real.size()) {
            return real.get(index);
        }
        return captured.get(index - real.size());
    }

    @NotNull
    @Override
    public Iterator<ParameterInfo> iterator() {
        return Iterables.concat(real, captured).iterator();
    }

    public static List<CapturedParamInfo> shiftAndAddStubs(List<CapturedParamInfo> capturedParams, int realSize) {
        List<CapturedParamInfo> result = new ArrayList<CapturedParamInfo>();
        for (CapturedParamInfo capturedParamInfo : capturedParams) {
            CapturedParamInfo newInfo = capturedParamInfo.newIndex(result.size() + realSize);
            result.add(newInfo);
            if (capturedParamInfo.getType().getSize() == 2) {
                result.add(CapturedParamInfo.STUB);
            }
        }
        return result;
    }

    public static List<ParameterInfo> addStubs(List<ParameterInfo> params) {
        List<ParameterInfo> result = new ArrayList<ParameterInfo>();
        for (ParameterInfo newInfo : params) {
            result.add(newInfo);
            if (newInfo.getType().getSize() == 2) {
                result.add(ParameterInfo.STUB);
            }
        }
        return result;
    }

    public ArrayList<Type> getCapturedTypes() {
        ArrayList<Type> result = new ArrayList<Type>();
        for (CapturedParamInfo info : captured) {
            if(info != CapturedParamInfo.STUB) {
                result.add(info.getType());
            }
        }
        return result;
    }
}
