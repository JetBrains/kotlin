// !DIAGNOSTICS: -UNUSED_VARIABLE

@Deprecated("", level = DeprecationLevel.HIDDEN)
open class Foo

fun test(f: Foo) {
    f.toString()
    val g: Foo? = <!HIDDEN!>Foo<!>()
}

class Bar : <!HIDDEN!>Foo<!>()
