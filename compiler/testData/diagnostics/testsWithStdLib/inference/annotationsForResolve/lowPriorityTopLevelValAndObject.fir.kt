// DIAGNOSTICS: -EXTENSION_SHADOWED_BY_MEMBER

// FILE: a.kt
package a

val bar get() = ""

@Suppress("INVISIBLE_MEMBER", <!ERROR_SUPPRESSION!>"INVISIBLE_REFERENCE"<!>)
@kotlin.internal.LowPriorityInOverloadResolution
val baz get() = ""

// FILE: b.kt
package b

object bar
object baz {
    val qux = 1
}

// FILE: test.kt
import a.*
import b.*

@Suppress("INVISIBLE_MEMBER", <!ERROR_SUPPRESSION!>"INVISIBLE_REFERENCE"<!>)
class Foo {
    @kotlin.internal.LowPriorityInOverloadResolution
    val bar = 1

    @kotlin.internal.LowPriorityInOverloadResolution
    val baz = 1
}

fun Foo.test() {
    bar.length
    baz.qux
}