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

package org.jetbrains.kotlin.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.descriptors.CallableDescriptor;
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor;
import org.jetbrains.kotlin.descriptors.PropertyDescriptor;
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor;
import org.jetbrains.kotlin.types.JetType;
import org.jetbrains.kotlin.types.TypesPackage;
import org.jetbrains.kotlin.types.checker.JetTypeChecker;

import java.util.List;

import static org.jetbrains.kotlin.resolve.OverridingUtil.OverrideCompatibilityInfo.Result.INCOMPATIBLE;

public class OverloadUtil {

    /**
     * Does not check names.
     */
    public static OverloadCompatibilityInfo isOverloadable(CallableDescriptor a, CallableDescriptor b) {
        int abc = braceCount(a);
        int bbc = braceCount(b);
        
        if (abc != bbc) {
            return OverloadCompatibilityInfo.success();
        }
        
        OverridingUtil.OverrideCompatibilityInfo overrideCompatibilityInfo = isOverloadableBy(a, b);
        switch (overrideCompatibilityInfo.getResult()) {
            case OVERRIDABLE:
            case CONFLICT:
                return OverloadCompatibilityInfo.someError();
            case INCOMPATIBLE:
                return OverloadCompatibilityInfo.success();
            default:
                throw new IllegalStateException();
        }
    }

    @NotNull
    private static OverridingUtil.OverrideCompatibilityInfo isOverloadableBy(
            @NotNull CallableDescriptor superDescriptor,
            @NotNull CallableDescriptor subDescriptor
    ) {
        OverridingUtil.OverrideCompatibilityInfo
                receiverAndParameterResult = OverridingUtil.checkReceiverAndParameterCount(superDescriptor, subDescriptor);
        if (receiverAndParameterResult != null) {
            return receiverAndParameterResult;
        }

        List<JetType> superValueParameters = OverridingUtil.compiledValueParameters(superDescriptor);
        List<JetType> subValueParameters = OverridingUtil.compiledValueParameters(subDescriptor);

        for (int i = 0; i < superValueParameters.size(); ++i) {
            JetType superValueParameterType = OverridingUtil.getUpperBound(superValueParameters.get(i));
            JetType subValueParameterType = OverridingUtil.getUpperBound(subValueParameters.get(i));
            if (!JetTypeChecker.DEFAULT.equalTypes(superValueParameterType, subValueParameterType)
                || TypesPackage.oneMoreSpecificThanAnother(subValueParameterType, superValueParameterType)) {
                return OverridingUtil.OverrideCompatibilityInfo
                        .valueParameterTypeMismatch(superValueParameterType, subValueParameterType, INCOMPATIBLE);
            }
        }

        return OverridingUtil.OverrideCompatibilityInfo.success();
    }

    private static int braceCount(CallableDescriptor a) {
        if (a instanceof PropertyDescriptor) {
            return 0;
        }
        else if (a instanceof SimpleFunctionDescriptor) {
            return 1;
        }
        else if (a instanceof ConstructorDescriptor) {
            return 1;
        }
        else {
            throw new IllegalStateException();
        }
    }

    public static class OverloadCompatibilityInfo {

        private static final OverloadCompatibilityInfo SUCCESS = new OverloadCompatibilityInfo(true, "SUCCESS");
        
        public static OverloadCompatibilityInfo success() {
            return SUCCESS;
        }
        
        public static OverloadCompatibilityInfo someError() {
            return new OverloadCompatibilityInfo(false, "XXX");
        }
        

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        private final boolean isSuccess;
        private final String message;

        public OverloadCompatibilityInfo(boolean success, String message) {
            isSuccess = success;
            this.message = message;
        }

        public boolean isSuccess() {
            return isSuccess;
        }

        public String getMessage() {
            return message;
        }

    }

}
