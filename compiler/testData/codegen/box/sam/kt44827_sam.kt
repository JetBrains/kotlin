// TARGET_BACKEND: JVM
// WITH_STDLIB
// FILE: test.kt

fun invoke(j: J): String {
    // Check that there's something sensible in the EnclosingMethod; crashes if it's not the case.
    j.javaClass.enclosingMethod

    return j()
}

class A(val result: String)

fun box(): String {
    var a = A("OK")
    return 42.let {
        invoke(a::result)
    }
}

// FILE: J.java

public interface J {
    String invoke();
}
