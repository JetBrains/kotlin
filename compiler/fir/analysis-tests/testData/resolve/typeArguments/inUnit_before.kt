// ISSUE: KT-84280, KT-84281
// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: -ForbidUselessTypeArgumentsIn25, +ProperSupportOfInnerClassesInCallableReferenceLHS

object MyUnit

typealias UnitT = Unit
typealias MyUnitT = MyUnit

fun <T> T.foo() { }
fun Unit.unitFoo() { }
fun MyUnit.myUnitFoo() { }

fun <T> bar(t: T) { }
fun unitBar(u: Unit) { }
fun myUnitBar(u: MyUnit) { }

fun <T> test() {
    Unit<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS_WARNING!><Any><!>
    MyUnit<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!><Any><!>

    Unit<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS_WARNING!><<!UNRESOLVED_REFERENCE!>Unresolved<!>><!>
    MyUnit<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!><<!UNRESOLVED_REFERENCE!>Unresolved<!>><!>

    Unit<Int>.<!UNRESOLVED_REFERENCE!>foo<!>()
    MyUnit<Int>.<!UNRESOLVED_REFERENCE!>foo<!>()

    Unit<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS_WARNING!><Int><!><!UNNECESSARY_SAFE_CALL!>?.<!>foo()
    MyUnit<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!><Int><!><!UNNECESSARY_SAFE_CALL!>?.<!>foo()

    Unit<String>.<!UNRESOLVED_REFERENCE!>unitFoo<!>()
    MyUnit<String>.<!UNRESOLVED_REFERENCE!>myUnitFoo<!>()

    bar(Unit<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS_WARNING!><Int><!>)
    bar(MyUnit<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!><Int><!>)

    unitBar(Unit<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS_WARNING!><Int><!>)
    myUnitBar(MyUnit<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!><Int><!>)

    (Unit<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS_WARNING!><Int><!>)
    (MyUnit<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!><Int><!>)

    (<!UNRESOLVED_REFERENCE!>Unit<!>)<Int>
    (<!UNRESOLVED_REFERENCE!>MyUnit<!>)<Int>

    Unit<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS_WARNING!><_><!>
    MyUnit<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!><_><!>

    Unit<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS_WARNING!><T&Any><!>
    MyUnit<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!><T&Any><!>

    Unit<Int>::<!UNRESOLVED_REFERENCE!>foo<!>
    MyUnit<Int>::<!UNRESOLVED_REFERENCE!>foo<!>

    Unit<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int><!>?::foo
    MyUnit<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int><!>?::foo

    Unit<Int>::<!UNRESOLVED_REFERENCE!>unitFoo<!>
    MyUnit<Int>::<!UNRESOLVED_REFERENCE!>myUnitFoo<!>

    (Unit<Int>)::<!UNRESOLVED_REFERENCE!>unitFoo<!>
    (MyUnit<Int>)::<!UNRESOLVED_REFERENCE!>myUnitFoo<!>

    Unit<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int><!>::class
    MyUnit<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int><!>::class

    (Unit<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int><!>)::class
    (MyUnit<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int><!>)::class

    Unit<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS_WARNING!><Int, Char, String><!>
    MyUnit<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!><Int, Char, String><!>

    Unit<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><T&Any><!>::class
    MyUnit<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><T&Any><!>::class
}

fun testTypeAlias() {
    UnitT<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS_WARNING!><Any><!>
    MyUnitT<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS_WARNING!><Any><!>

    bar(UnitT<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS_WARNING!><Any><!>)
    bar(MyUnitT<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS_WARNING!><Any><!>)

    unitBar(UnitT<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS_WARNING!><Any><!>)
    myUnitBar(<!ARGUMENT_TYPE_MISMATCH!>MyUnitT<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS_WARNING!><Any><!><!>)
}

/* GENERATED_FIR_TAGS: callableReference, classReference, funWithExtensionReceiver, functionDeclaration, nullableType,
objectDeclaration, safeCall, typeAliasDeclaration, typeParameter */
