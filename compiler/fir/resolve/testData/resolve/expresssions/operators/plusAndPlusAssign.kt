class Foo {
    operator fun plus(f: Foo): Foo {}
    operator fun plusAssign(f: Foo) {}
}

fun test() {
    var f = Foo()
    <!ASSIGN_OPERATOR_AMBIGUITY, ASSIGN_OPERATOR_AMBIGUITY, ASSIGN_OPERATOR_AMBIGUITY!>f += f<!>
}