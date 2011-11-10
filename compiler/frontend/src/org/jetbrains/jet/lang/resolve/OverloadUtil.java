package org.jetbrains.jet.lang.resolve;

import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;

/**
 * @author Stepan Koltsov
 */
public class OverloadUtil {

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
