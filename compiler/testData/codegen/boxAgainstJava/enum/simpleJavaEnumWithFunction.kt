// FILE: test/En.java

package test;

import java.lang.Override;
import java.lang.String;

public enum En {
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

// FILE: 1.kt

import test.En.*

fun box() =
    if (A.repr() == "A" && B.repr() == "olololB") "OK"
    else "fail"
