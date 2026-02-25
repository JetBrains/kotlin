// ISSUE: KT-84280, KT-84281
// RUN_PIPELINE_TILL: FRONTEND

object MyUnit

typealias UnitT = Unit
typealias MyUnitT = MyUnit

fun <T> T.foo() { }
fun Unit.unitFoo() { }
fun MyUnit.myUnitFoo() { }

fun <T> bar(t: T) { }
fun unitBar(u: Unit) { }
fun myUnitBar(u: MyUnit) { }

fun test() {
    Unit<Any>
    MyUnit<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!><Any><!>

    Unit<<!UNRESOLVED_REFERENCE!>Unresolved<!>>
    MyUnit<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!><<!UNRESOLVED_REFERENCE!>Unresolved<!>><!>

    Unit<Int>.<!UNRESOLVED_REFERENCE!>foo<!>()
    MyUnit<Int>.<!UNRESOLVED_REFERENCE!>foo<!>()

    Unit<String>.<!UNRESOLVED_REFERENCE!>unitFoo<!>()
    MyUnit<String>.<!UNRESOLVED_REFERENCE!>myUnitFoo<!>()

    bar(Unit<Int>)
    bar(MyUnit<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!><Int><!>)

    unitBar(Unit<Int>)
    myUnitBar(MyUnit<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!><Int><!>)

    Unit<Int>::<!UNRESOLVED_REFERENCE!>foo<!>
    MyUnit<Int>::<!UNRESOLVED_REFERENCE!>foo<!>

    Unit<Int>::<!UNRESOLVED_REFERENCE!>unitFoo<!>
    MyUnit<Int>::<!UNRESOLVED_REFERENCE!>myUnitFoo<!>

    Unit<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int><!>::class
    MyUnit<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int><!>::class

    Unit<Int, Char, String>
    MyUnit<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!><Int, Char, String><!>
}

fun testTypeAlias() {
    UnitT<Any>
    MyUnitT<Any>

    bar(UnitT<Any>)
    bar(MyUnitT<Any>)

    unitBar(UnitT<Any>)
    myUnitBar(<!ARGUMENT_TYPE_MISMATCH!>MyUnitT<Any><!>)
}

/* GENERATED_FIR_TAGS: classReference, funWithExtensionReceiver, functionDeclaration, nullableType, objectDeclaration,
typeParameter */
