fun foo1() {}
@Deprecated("Use foo3 instead") fun foo2() {}
fun foo3() {}

fun test() {
    foo<caret>
}

// ORDER: foo1, foo3, foo2