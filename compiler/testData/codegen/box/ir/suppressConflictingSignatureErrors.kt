// This test checks that suppressing conflicting signature / accidental override errors works.
// This is used in libraries for binary-compatible migration, as well as in some cases in multiplatform projects.
// TARGET_BACKEND: JVM
// IGNORE_BACKEND_FIR: JVM_IR
// FILE: box.kt

open class B {
    open val s: String
        get() = "Fail"
}

class C : B() {
    @Suppress("ACCIDENTAL_OVERRIDE")
    fun getS(): String = "O"
}

fun box(): String = C().getS() + D().getS()

// FILE: another.kt

@file:Suppress("ACCIDENTAL_OVERRIDE")

class D : B() {
    fun getS(): String = "K"
}
