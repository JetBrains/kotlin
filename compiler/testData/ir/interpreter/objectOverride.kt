@CompileTimeCalculation
interface Object {
    fun get(): String

    @CompileTimeCalculation
    fun defaultGet() = "Object"
}

object A : Object {
    @CompileTimeCalculation
    override fun get() = "A"
}

open class B : Object {
    @CompileTimeCalculation
    override fun get() = "B"
}

object C : B() {
    @CompileTimeCalculation
    override fun get() = "Default: " + super.defaultGet() + "; from super B: " + super.get() + "; from current: " + "companion C"
}

const val a1 = A.<!EVALUATED: `Object`!>defaultGet()<!>
const val a2 = A.<!EVALUATED: `A`!>get()<!>
const val c1 = C.<!EVALUATED: `Object`!>defaultGet()<!>
const val c2 = C.<!EVALUATED: `Default: Object; from super B: B; from current: companion C`!>get()<!>
