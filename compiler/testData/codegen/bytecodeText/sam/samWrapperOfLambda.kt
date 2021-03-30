// KOTLIN_CONFIGURATION_FLAGS: SAM_CONVERSIONS=INDY
// FILE: JFoo.java

public class JFoo {
    public static void foo(Runnable f) {
        f.run();
    }
}

// FILE: Test.kt
fun test() {
    JFoo.foo({})
}

// Lambda inlined into run(), no wrapper class generated:
// JVM_TEMPLATES:
// 1 NEW
// 0 INVOKEINTERFACE

// JVM_IR_TEMPLATES:
// 0 NEW
// 0 INVOKEINTERFACE
