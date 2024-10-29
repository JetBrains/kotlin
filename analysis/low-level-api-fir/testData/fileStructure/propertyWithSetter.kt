abstract class Foo {
    abstract var id: Int
        protected set
}

class Bar : Foo() {
    override var id: Int = 1
    public set
}

fun test() {
    val bar = Bar()
    bar.id = 1
}
