// ISSUE: KT-66558
// WITH_STDLIB

object O {
    const val X = 42
    const val ITX = /*implicit this.*/X
    const val TX = this.X
    const val OX = O.X
}
val OCopy get() = O.also { print("Side effect") }

// K2: should be error - the constant value must be computed at compile time
const val X = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>OCopy.X<!>

val Y = O.X
const val OXplus1 = O.X + 1
const val Yplus1 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>Y + 1<!>

// K2: should be error - default value of annotation parameter must be computed at compile time
annotation class Ann(val x: Int = <!ANNOTATION_PARAMETER_DEFAULT_VALUE_MUST_BE_CONSTANT!>OCopy.X<!>)

// K2: should be error - the value of annotation parameter must be computed at compile time
@Ann(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>OCopy.X<!>)
fun foo() {}

class C {
    val Z = 0
    @Ann(<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>Z<!>)
    fun implicitThis() {}
    @Ann(<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>this.Z<!>)
    fun explicitThis() {}
}
