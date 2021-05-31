// FIR_COMPARISON
class Foo(val bar: Bar)
class Bar(val baz: String)

fun foo(foo: Foo) {
    val s = "$foo.bar.<caret>"
}

// EXIST: baz