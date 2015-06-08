package fwAbstractProperty

// Breakpoint at getter/setter
abstract class MyAbstractClass {
    //FieldWatchpoint! (propVal)
    abstract val propVal: Int

    //FieldWatchpoint! (propVar)
    abstract var propVar: Int

    fun testPropertyInAbstractClass() {
        propVal
        propVar
        propVar = 2
    }
}

class MyAbstractClassImpl : MyAbstractClass() {
    override val propVal = 1
    override var propVar = 1

    fun testPropertyInAbstractClassImpl() {
        propVal
        propVar
        propVar = 2
    }
}

abstract class MyAbstractClassWithoutBreakpoints {
    abstract val propVal2: Int
    abstract var propVar2: Int

    fun testPropertyInAbstractClass() {
        propVal2
        propVar2
        propVar2 = 2
    }
}

// Breakpoint at GETFILED/PUTFIELD
class MyAbstractClassImplWithBreakpoints : MyAbstractClassWithoutBreakpoints() {
    //FieldWatchpoint! (propVal2)
    override val propVal2 = 1

    //FieldWatchpoint! (propVar2)
    override var propVar2 = 1

    fun testPropertyInAbstractClassImpl() {
        propVal2
        propVar2
        propVar2 = 2
    }
}

fun main(args: Array<String>) {
    val mac = object: MyAbstractClass() {
        override val propVal: Int get() = 1
        override var propVar: Int
            get() = 1
            set(value) {
            }
    }
    mac.testPropertyInAbstractClass()

    val maci = MyAbstractClassImpl()
    maci.testPropertyInAbstractClass()
    maci.testPropertyInAbstractClassImpl()

    val macwb = object: MyAbstractClassWithoutBreakpoints() {
        override val propVal2: Int get() = 1
        override var propVar2: Int
            get() = 1
            set(value) {
            }
    }
    macwb.testPropertyInAbstractClass()

    val macwbi = MyAbstractClassImplWithBreakpoints()
    macwbi.testPropertyInAbstractClass()
    macwbi.testPropertyInAbstractClassImpl()
}

// RESUME: 17