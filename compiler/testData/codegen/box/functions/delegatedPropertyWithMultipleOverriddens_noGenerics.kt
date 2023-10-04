// ISSUE: KT-55828
// DUMP_IR
// IGNORE_CODEGEN_WITH_IR_FAKE_OVERRIDE_GENERATION: extra overridden symbols for declarations in MyMutableList.
//   ^ This is most likely not a problem, and IR dump can be changed once IR fake override generation is enabled by default.

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
