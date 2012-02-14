/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.resolve;

import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;

/**
 * @author Stepan Koltsov
 */
public class OverloadUtil {

    /**
     * Does not check names.
     */
    public static OverloadCompatibilityInfo isOverloadble(FunctionDescriptor a, FunctionDescriptor b) {
        OverridingUtil.OverrideCompatibilityInfo overrideCompatibilityInfo = OverridingUtil.isOverridableByImpl(a, b, false);
        if (overrideCompatibilityInfo.isSuccess()) {
            return OverloadCompatibilityInfo.someError(); 
        } else {
            return OverloadCompatibilityInfo.success();
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
