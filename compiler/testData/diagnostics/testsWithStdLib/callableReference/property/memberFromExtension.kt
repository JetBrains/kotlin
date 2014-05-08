class A {
    val foo: Unit = Unit.VALUE
    var bar: String = ""
    var self: A
        get() = this
        set(value) { }
}
    
fun A.test() {
    val x = ::foo
    val y = ::bar
    val z = ::self

    x : KMemberProperty<A, Unit>
    y : KMutableMemberProperty<A, String>
    z : KMutableMemberProperty<A, A>

    y.set(z.get(A()), x.get(A()).toString())
}
