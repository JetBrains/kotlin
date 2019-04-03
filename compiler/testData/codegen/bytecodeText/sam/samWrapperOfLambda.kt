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
// 1 NEW
// 0 INVOKEINTERFACE
