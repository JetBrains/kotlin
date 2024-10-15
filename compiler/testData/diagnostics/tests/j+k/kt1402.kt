// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// FILE: a/M.java
package a;

public class M {
    public static class Inner {
        private int i;
        public Inner(int i) {
            this.i = i;
        }

        public int getI() {
            return i;
        }
    }
}

// FILE: b.kt
package b

import a.M.Inner

fun foo() {
    doSmth(Inner(87))
}

fun doSmth(b: Inner) = b
