// See KT-8269 java.lang.IllegalAccessError on accessing protected inner class declared in Kotlin super class
// TARGET_BACKEND: JVM
// FILE: Test.kt

package com.company

import other.JavaClass

open class Test {
    protected class ProtectedClass
}

fun box(): String {
    JavaClass.test()
    return "OK"
}

// FILE: other/JavaClass.java

package other;

import com.company.Test;

public class JavaClass {
    static class JavaTest extends Test {
        public static boolean foo(Object obj) {
            return obj instanceof ProtectedClass;
        }
    }

    public static void test() {
        JavaTest.foo(new Object());
    }
}
