// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// LAMBDAS: INDY
// WITH_STDLIB
// FILE: J.java
public class J implements I {
    @Override
    public String f() {
        return null;
    }
}

// FILE: box.kt
interface I {
    fun f(): String
}

fun test(i: I) {
    // For compatibility with class-based lambdas, we don't generate nullability assertions for values captured into indy lambdas.
    // E.g. in this case, x has type String but contains null, which is incorrect from the Kotlin's type system point of view,
    // but it shouldn't lead to an NPE, so that behavior would be the same with class-based and indy lambdas.
    val x = i.f()
    val y = { x }
    y.invoke()
}

fun box(): String {
    test(J())
    return "OK"
}
