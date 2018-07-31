// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_RUNTIME
// FILE: JavaClass.java
class JavaClass {


    public static String test()
    {
        return TestApp.getValue();
    }
}

// FILE: Kotlin.kt
open class TestApp {
    companion object {
        @JvmStatic
        var value: String = "OK"
            private set
    }
}


fun box(): String {
    return JavaClass.test()
}
