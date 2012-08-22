package test;

import java.lang.Override;
import java.lang.String;

public enum simpleJavaEnumWithFunction {
    A {
        @Override
        public String repr() {
            return "A";
        }
    },
    B;
    
    public String repr() {
        return "ololol" + toString();
    }
}
