class ClassWithCompanion {
    companion object {
        fun foo() { }
    }
}

/**
 * [ClassWithCompanion.f<caret>oo]
 */
fun test() { }
