// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

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
