// KT-3563 Compiler requiring java.io.File, and it's unclear why

package bar

import java.io.File

class Customer(name: String)

fun foo(f: File, c: Customer) {
    f.name

    c.<!UNRESOLVED_REFERENCE!>name<!> // name should be unresolved here
}

//from standard library
val File.name: String
    get() = getName()
