class A {
    val foo: Int = 42
    var bar: String = ""
}

fun test() {
    val p = A::foo

    p : KMemberProperty<A, Int>
    <!TYPE_MISMATCH!>p<!> : KMutableMemberProperty<A, Int>
    p.get(A()) : Int
    p.get(<!NO_VALUE_FOR_PARAMETER!>)<!>
    p.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>set<!>(A(), 239)

    val q = A::bar

    q : KMemberProperty<A, String>
    q : KMutableMemberProperty<A, String>
    q.get(A()): String
    q.set(A(), "q")
}
