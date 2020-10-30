// JVM_TARGET: 1.8
// TARGET_BACKEND: JVM

// WITH_RUNTIME
// FILE: Test.java

class Test {

    public static String test1() {
        return A.sayHello();
    }
}

// FILE: simpleCompanionObject.kt

interface A {
    companion object {
        @JvmStatic
        @JvmOverloads
        fun sayHello(greeting: String = "OK"): String {
            return greeting
        }
    }
}
fun box(): String {
    if (Test.test1() != "OK") return "fail 1"

    return "OK"
}