// TARGET_BACKEND: JVM
// WITH_STDLIB
// JVM_ABI_K1_K2_DIFF: KT-63984

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
