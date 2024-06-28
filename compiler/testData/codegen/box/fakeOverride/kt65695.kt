// TARGET_BACKEND: JVM

// FILE: A.kt
interface A {
    val x: String
}

// FILE: B.kt
interface B : A

// FILE: C.java
public interface C extends B { }

// FILE: D.kt
class D : C {
    override val x: String
        get() = "OK"
}

// FILE: box.kt
fun go(p: C): String = p.x

fun box(): String = go(D())