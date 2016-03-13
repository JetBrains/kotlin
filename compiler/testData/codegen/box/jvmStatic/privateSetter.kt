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