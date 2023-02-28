// ISSUE: KT-55828
// DUMP_IR
interface MyCollection {
    fun foo(): String
    val bar: String
}

interface MyList : MyCollection {
    override fun foo(): String
    override val bar: String
}

interface MyMutableCollection : MyCollection
interface MyMutableList : MyList, MyMutableCollection

abstract class MyAbstractCollection protected constructor() : MyCollection {
    abstract override fun foo(): String
    abstract override val bar: String
}

class MyArrayList : MyMutableList, MyAbstractCollection() {
    override fun foo(): String = "O"
    override val bar: String = "K"
}

class MC : MyMutableCollection by MyArrayList()

fun box(): String {
    val x = MC()
    return x.foo() + x.bar
}
