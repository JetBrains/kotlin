// ISSUE: KT-84589
// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ForbidArrayOfNothingInLhsOfClassLiteral
// WITH_REFLECT

fun test() {
    <!UNSUPPORTED!>Array<Nothing><!>::class
    println(<!UNSUPPORTED!>Array<Nothing><!>::class)
    <!UNSUPPORTED!>Array<Nothing><!>::class.simpleName
    <!UNSUPPORTED!>Array<Nothing><!>::class.<!UNSUPPORTED, UNSUPPORTED!>java<!>

    <!UNSUPPORTED!>Array<Nothing?><!>::class
    println(<!UNSUPPORTED!>Array<Nothing?><!>::class)
    <!UNSUPPORTED!>Array<Nothing?><!>::class.simpleName
    <!UNSUPPORTED!>Array<Nothing?><!>::class.<!UNSUPPORTED, UNSUPPORTED!>java<!>
}

typealias MyNothing = Nothing

fun foo(): <!UNSUPPORTED!>Array<MyNothing><!> = TODO()

fun taTest() {
    <!UNSUPPORTED!>foo<!>()::class // no double reporting

    <!UNSUPPORTED!>Array<MyNothing><!>::class
    println(<!UNSUPPORTED!>Array<MyNothing><!>::class)
    <!UNSUPPORTED!>Array<MyNothing><!>::class.simpleName
    <!UNSUPPORTED!>Array<MyNothing><!>::class.<!UNSUPPORTED, UNSUPPORTED!>java<!>

    <!UNSUPPORTED!>Array<MyNothing?><!>::class
    println(<!UNSUPPORTED!>Array<MyNothing?><!>::class)
    <!UNSUPPORTED!>Array<MyNothing?><!>::class.simpleName
    <!UNSUPPORTED!>Array<MyNothing?><!>::class.<!UNSUPPORTED, UNSUPPORTED!>java<!>
}

fun projectionsTest() {
    <!UNSUPPORTED!>Array<in Nothing><!>::class
    <!UNSUPPORTED!>Array<in Nothing?><!>::class
    <!UNSUPPORTED!>Array<in MyNothing><!>::class
    <!UNSUPPORTED!>Array<in MyNothing?><!>::class

    <!UNSUPPORTED!>Array<out Nothing><!>::class
    <!UNSUPPORTED!>Array<out MyNothing?><!>::class
}

/* GENERATED_FIR_TAGS: classReference, functionDeclaration, nullableType */
