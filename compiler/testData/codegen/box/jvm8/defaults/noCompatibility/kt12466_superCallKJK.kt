// TARGET_BACKEND: JVM
// JVM_DEFAULT_MODE: no-compatibility
// FILE: A.kt
interface A {
    fun f(): String = "O"
}

// FILE: B.java
public interface B extends A {}

// FILE: box.kt
class C : B {
    override fun f(): String = super.f() + "K"
}

fun box(): String = C().f()
