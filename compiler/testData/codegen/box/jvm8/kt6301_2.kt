// !LANGUAGE: +JvmStaticInInterface
// WITH_RUNTIME
// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// FILE: test.kt
fun box(): String {
    return Java.test()
}

interface Foo {

    companion object {
        @JvmStatic
        fun foo() = "O"


        val fooProp = "K"
            @JvmStatic get
    }
}

// FILE: Java.java
public class Java {
    public static String test() {
        return Foo.foo() + Foo.getFooProp();
    }
}