// ISSUE: KT-84280, KT-84281
// RUN_PIPELINE_TILL: FRONTEND
// LATEST_LV_DIFFERENCE

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
    Unit<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!><Any><!>
    MyUnit<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!><Any><!>

    Unit<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!><<!UNRESOLVED_REFERENCE!>Unresolved<!>><!>
    MyUnit<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!><<!UNRESOLVED_REFERENCE!>Unresolved<!>><!>

    Unit<Int>.<!UNRESOLVED_REFERENCE!>foo<!>()
    MyUnit<Int>.<!UNRESOLVED_REFERENCE!>foo<!>()

    Unit<String>.<!UNRESOLVED_REFERENCE!>unitFoo<!>()
    MyUnit<String>.<!UNRESOLVED_REFERENCE!>myUnitFoo<!>()

    bar(Unit<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!><Int><!>)
    bar(MyUnit<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!><Int><!>)

    unitBar(Unit<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!><Int><!>)
    myUnitBar(MyUnit<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!><Int><!>)

    Unit<Int>::<!UNRESOLVED_REFERENCE!>foo<!>
    MyUnit<Int>::<!UNRESOLVED_REFERENCE!>foo<!>

    Unit<Int>::<!UNRESOLVED_REFERENCE!>unitFoo<!>
    MyUnit<Int>::<!UNRESOLVED_REFERENCE!>myUnitFoo<!>

    Unit<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int><!>::class
    MyUnit<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int><!>::class

    Unit<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!><Int, Char, String><!>
    MyUnit<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!><Int, Char, String><!>
}

fun testTypeAlias() {
    UnitT<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!><Any><!>
    MyUnitT<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!><Any><!>

    bar(UnitT<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!><Any><!>)
    bar(MyUnitT<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!><Any><!>)

    unitBar(UnitT<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!><Any><!>)
    myUnitBar(<!ARGUMENT_TYPE_MISMATCH!>MyUnitT<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!><Any><!><!>)
}

/* GENERATED_FIR_TAGS: classReference, funWithExtensionReceiver, functionDeclaration, nullableType, objectDeclaration,
typeParameter */
