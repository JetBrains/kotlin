open class Foo() {
    public fun k(): String = "K"
}


trait T: Foo {
    public fun xyzzy(): String = o() + k()
    public fun o(): String
}

class TImpl(): Foo(), T {
    public override fun o(): String = "O"
}

fun box(): String {
    return TImpl().xyzzy()
}
