// FILE: ClassWithCompanion.kt

class ClassWithCompanion {
    companion object {
        fun foo() { }
    }
}

// FILE: main.kt

/**
 * [ClassWithCompanion.f<caret>oo]
 */
fun test() { }
