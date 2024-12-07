// FILE: ClassWithCompanion.kt
package bar

class ClassWithCompanion {
    companion object {
        fun foo() { }
    }
}

// FILE: main.kt
import bar.ClassWithCompanion

/**
 * [ClassWithCompanion.f<caret>oo]
 */
fun test() { }
