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

        public ShiftRemapper(int shift) {
            this.shift = shift;
        }

        @Override
        public int doRemap(int index) {
            return index + shift;
        }
    }

    public static class ClosureRemapper extends ShiftRemapper {

        private final ClosureInfo info;
        private final List<ParameterInfo> originalParams;
        private final int capturedSize;

        public ClosureRemapper(ClosureInfo info, int valueParametersShift, List<ParameterInfo> originalParams) {
            super(valueParametersShift);
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

        private final int paramShift;
        private final int paramSize;
        private final int additionalParamsSize;
        private final List<ParameterInfo> params;
        private final int actualParams;

        public ParamRemapper(int paramShift, int paramSize, int additionalParamsSize, List<ParameterInfo> params) {
            this.paramShift = paramShift;
            this.paramSize = paramSize;
            this.additionalParamsSize = additionalParamsSize;
            this.params = params;

            int paramCount = 0;
            for (int i = 0; i < params.size(); i++) {
                ParameterInfo info = params.get(i);
                if (!info.isSkippedOrRemapped()) {
                    paramCount += info.getType().getSize();
                }
            }
            actualParams = paramCount;
        }

        @Override
        public int doRemap(int index) {
            if (index < paramSize) {
                ParameterInfo info = params.get(index);
                if (info.isSkipped) {
                    throw new RuntimeException("Trying to access skipped parameter: " + info.type + " at " +index);
                }
                return info.getInlinedIndex();
            } else {
                return paramShift + actualParams + index; //captured params not used directly in this inlined method, they used in closure
            }
        }
    }



    protected boolean nestedRemmapper;

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

}
