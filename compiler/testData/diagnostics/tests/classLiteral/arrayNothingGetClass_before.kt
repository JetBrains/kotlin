// ISSUE: KT-84589
// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: -ForbidArrayOfNothingInLhsOfClassLiteral
// WITH_REFLECT

fun test() {
    <!UNSUPPORTED_ARRAY_OF_NOTHING_IN_CLASS_LITERAL_LHS!>Array<Nothing><!>::class
    println(<!UNSUPPORTED_ARRAY_OF_NOTHING_IN_CLASS_LITERAL_LHS!>Array<Nothing><!>::class)
    <!UNSUPPORTED_ARRAY_OF_NOTHING_IN_CLASS_LITERAL_LHS!>Array<Nothing><!>::class.simpleName
    <!UNSUPPORTED_ARRAY_OF_NOTHING_IN_CLASS_LITERAL_LHS!>Array<Nothing><!>::class.<!UNSUPPORTED, UNSUPPORTED!>java<!>

    <!UNSUPPORTED_ARRAY_OF_NOTHING_IN_CLASS_LITERAL_LHS!>Array<Nothing?><!>::class
    println(<!UNSUPPORTED_ARRAY_OF_NOTHING_IN_CLASS_LITERAL_LHS!>Array<Nothing?><!>::class)
    <!UNSUPPORTED_ARRAY_OF_NOTHING_IN_CLASS_LITERAL_LHS!>Array<Nothing?><!>::class.simpleName
    <!UNSUPPORTED_ARRAY_OF_NOTHING_IN_CLASS_LITERAL_LHS!>Array<Nothing?><!>::class.<!UNSUPPORTED, UNSUPPORTED!>java<!>
}

typealias MyNothing = Nothing

fun foo(): <!UNSUPPORTED!>Array<MyNothing><!> = TODO()

fun taTest() {
    <!UNSUPPORTED!>foo<!>()::class // no double reporting

    <!UNSUPPORTED_ARRAY_OF_NOTHING_IN_CLASS_LITERAL_LHS!>Array<MyNothing><!>::class
    println(<!UNSUPPORTED_ARRAY_OF_NOTHING_IN_CLASS_LITERAL_LHS!>Array<MyNothing><!>::class)
    <!UNSUPPORTED_ARRAY_OF_NOTHING_IN_CLASS_LITERAL_LHS!>Array<MyNothing><!>::class.simpleName
    <!UNSUPPORTED_ARRAY_OF_NOTHING_IN_CLASS_LITERAL_LHS!>Array<MyNothing><!>::class.<!UNSUPPORTED, UNSUPPORTED!>java<!>

    <!UNSUPPORTED_ARRAY_OF_NOTHING_IN_CLASS_LITERAL_LHS!>Array<MyNothing?><!>::class
    println(<!UNSUPPORTED_ARRAY_OF_NOTHING_IN_CLASS_LITERAL_LHS!>Array<MyNothing?><!>::class)
    <!UNSUPPORTED_ARRAY_OF_NOTHING_IN_CLASS_LITERAL_LHS!>Array<MyNothing?><!>::class.simpleName
    <!UNSUPPORTED_ARRAY_OF_NOTHING_IN_CLASS_LITERAL_LHS!>Array<MyNothing?><!>::class.<!UNSUPPORTED, UNSUPPORTED!>java<!>
}

fun projectionsTest() {
    <!UNSUPPORTED_ARRAY_OF_NOTHING_IN_CLASS_LITERAL_LHS!>Array<in Nothing><!>::class
    <!UNSUPPORTED_ARRAY_OF_NOTHING_IN_CLASS_LITERAL_LHS!>Array<in Nothing?><!>::class
    <!UNSUPPORTED_ARRAY_OF_NOTHING_IN_CLASS_LITERAL_LHS!>Array<in MyNothing><!>::class
    <!UNSUPPORTED_ARRAY_OF_NOTHING_IN_CLASS_LITERAL_LHS!>Array<in MyNothing?><!>::class

    <!UNSUPPORTED_ARRAY_OF_NOTHING_IN_CLASS_LITERAL_LHS!>Array<out Nothing><!>::class
    <!UNSUPPORTED_ARRAY_OF_NOTHING_IN_CLASS_LITERAL_LHS!>Array<out MyNothing?><!>::class
}

/* GENERATED_FIR_TAGS: classReference, functionDeclaration, nullableType */
