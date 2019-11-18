// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM

// FILE: MyInt.java

public interface MyInt {

    String test();
}

// FILE: test.kt

interface A : MyInt {
    override public fun test(): String? {
        return "OK"
    }
}

class B: A

fun box() : String {
    return B().test()!!
}
