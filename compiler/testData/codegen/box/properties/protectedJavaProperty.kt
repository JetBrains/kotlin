// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_RUNTIME
// FILE: JavaBaseClass.java

public class JavaBaseClass {

    private String field = "fail";

    protected String getFoo() {
        return field;
    }

    protected void setFoo(String foo) {
        field = foo;
    }
}

// FILE: kotlin.kt

package z

import JavaBaseClass

object KotlinExtender : JavaBaseClass() {
    @JvmStatic fun test(): String {
        return runSlowly {
            foo = "OK"
            foo
        }
    }
}
fun runSlowly(f: () -> String): String {
    return f()
}

fun box(): String {
    return KotlinExtender.test()
}
