// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_K2: ANY
// ISSUE: KT-69421

// FILE: JavaInterface.java
public interface JavaInterface {
    default String foo() {
        return "Not OK";
    }
}

// FILE: main.kt
interface A {
    fun foo(): String
}

class AImpl: A {
    override fun foo(): String {
        return "OK"
    }
}

class Test(val a: A): JavaInterface, A by a

fun box(): String {
    return Test(AImpl()).foo()
}
