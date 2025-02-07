// SAM_CONVERSIONS: INDY
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
// 0 NEW
// 0 INVOKEINTERFACE
