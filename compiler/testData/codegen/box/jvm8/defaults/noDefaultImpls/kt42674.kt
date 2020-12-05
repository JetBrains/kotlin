// !JVM_DEFAULT_MODE: all
// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// WITH_RUNTIME
// FILE: A.java

public interface A {
    default String foo() {
        return "fail";
    }
}
// FILE: B.java

public abstract class B implements A {}

// FILE: test.kt

interface KDefault : A {
    override fun foo() = "OK"
}

class Problem : B(), KDefault

fun box(): String {
    return Problem().foo()
}