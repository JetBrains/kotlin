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

package org.jetbrains.jet.codegen.asm;

import java.util.List;

public abstract class VarRemapper {



    public static class ShiftRemapper extends VarRemapper {

        private final int shift;

        public ShiftRemapper(int shift, VarRemapper remapper) {
            super(remapper);
            this.shift = shift;
        }

        @Override
        public int doRemap(int index) {
            return index + shift;
        }
    }

    public static class ClosureRemapper extends ShiftRemapper {

        private final LambdaInfo info;
        private final List<ParameterInfo> originalParams;
        private final int capturedSize;

        public ClosureRemapper(LambdaInfo info, int valueParametersShift, List<ParameterInfo> originalParams) {
            super(valueParametersShift, null);
            this.info = info;
            this.originalParams = originalParams;
            capturedSize = info.getCapturedVarsSize();
        }

        @Override
        public int doRemap(int index) {
            if (index < capturedSize) {
                return originalParams.get(info.getParamOffset() + index).getInlinedIndex();
            }
            return super.doRemap(index - capturedSize);
        }
    }

    public static class ParamRemapper extends VarRemapper {

        private final int allParamsSize;
        private final Parameters params;
        private final int actualParamsSize;

        private final int [] remapIndex;

        public ParamRemapper(Parameters params, VarRemapper parent) {
            super(parent);
            this.allParamsSize = params.totalSize();
            this.params = params;

            int realSize = 0;
            remapIndex = new int [params.totalSize()];

            int index = 0;
            for (ParameterInfo info : params) {
                if (!info.isSkippedOrRemapped()) {
                    remapIndex[index] = realSize;
                    realSize += info.getType().getSize();
                } else {
                    remapIndex[index] = info.isRemapped() ? info.getRemapIndex() : -1;
                }
                index++;
            }

            actualParamsSize = realSize;
        }

        @Override
        public int doRemap(int index) {
            int remappedIndex;

            if (index < allParamsSize) {
                ParameterInfo info = params.get(index);
                remappedIndex = remapIndex[index];
                if (info.isSkipped || remappedIndex == -1) {
                    throw new RuntimeException("Trying to access skipped parameter: " + info.type + " at " +index);
                }
                if (info.isRemapped()) {
                    return remappedIndex;
                }
            } else {
                remappedIndex = actualParamsSize - params.totalSize() + index; //captured params not used directly in this inlined method, they used in closure
            }

            if (parent == null) {
                return remappedIndex;
            }

            return parent.doRemap(remappedIndex);
        }
    }

    protected boolean nestedRemmapper;

    protected VarRemapper parent;

    public VarRemapper(VarRemapper parent) {
        this.parent = parent;
    }

    public int remap(int index) {
        if (nestedRemmapper) {
            return index;
        }
        return doRemap(index);
    }

    abstract public int doRemap(int index);

    public void setNestedRemap(boolean nestedRemap) {
        nestedRemmapper = nestedRemap;
    }


    public static int getActualSize(List<ParameterInfo> params) {
        int size = 0;
        for (ParameterInfo paramInfo : params) {
            if (!paramInfo.isSkippedOrRemapped()) {
                size += paramInfo.getType().getSize();
            }
        }
        return size;
    }
}
