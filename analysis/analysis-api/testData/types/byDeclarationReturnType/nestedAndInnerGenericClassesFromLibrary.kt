// MODULE: library
// MODULE_KIND: LibraryBinary
// FILE: library.kt
package library

class A<AA> {
    class B<BB> {
        inner class C<CC>
    }
}

// MODULE: main(library)
// FILE: main.kt
import library.A

fun fo<caret>o(): A.B<String>.C<Int>? = null
