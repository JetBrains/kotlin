@CompileTimeCalculation
interface Object {
    fun get(): String

    //@CompileTimeCalculation
    fun defaultGet() = "Object"
}

@CompileTimeCalculation
open class A {
    companion object : Object {
        @CompileTimeCalculation
        override fun get() = "A"
    }
}

@CompileTimeCalculation
abstract class B : Object {
    fun str() = "B"
}

@CompileTimeCalculation
class C {
    companion object : B() {
        @CompileTimeCalculation
        override fun get() = "Default: " + super.defaultGet() + "; from super B: " + super.str() + "; from current: " + " companion C"
    }
}

const val a = A.<!EVALUATED: `A`!>get()<!>
const val c1 = C.<!EVALUATED: `Object`!>defaultGet()<!>
const val c2 = C.<!EVALUATED: `Default: Object; from super B: B; from current:  companion C`!>get()<!>
