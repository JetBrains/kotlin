// TARGET_BACKEND: JVM

// WITH_STDLIB
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

class A {
    @JvmField var foo = "fail"

    companion object : JavaBaseClass() {
        @JvmStatic fun test(): String {
            return runSlowly {
                foo = "OK"
                foo
            }
        }
    }
}
fun runSlowly(f: () -> String): String {
    return f()
}

fun box(): String {
    val a = A()
    a.foo = "Kotlin"
    if (a.foo != "Kotlin") return "fail"

    return A.test()
}
