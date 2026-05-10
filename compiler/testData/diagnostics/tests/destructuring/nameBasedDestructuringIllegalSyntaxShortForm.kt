// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +NameBasedDestructuring, +EnableNameBasedDestructuringShortForm

data class Person(val fullName: String, val age: Int)

class Foo {
    val aProp: Int = 1
}

val <!SYNTAX!>(name = fullName)<!> = <!UNRESOLVED_REFERENCE!>person<!>

fun destructureEmptyShort(person: Person) {
    val (<!SYNTAX!><!>) = person
}

fun destructureEmptyRenameFull(person: Person) {
    val (<!SYNTAX!><!> = <!UNRESOLVED_REFERENCE!>age<!><!SYNTAX!>) = person<!>
}

fun destructureWithConst(person: Person) {
    val (<!WRONG_MODIFIER_TARGET!>const<!> fullName) = person
    var (<!WRONG_MODIFIER_TARGET!>const<!> age) = person
}

fun destructureWithPrivate(person: Person) {
    val (<!WRONG_MODIFIER_TARGET!>private<!> fullName) = person
    var (<!WRONG_MODIFIER_TARGET!>private<!> age) = person
}

fun destructureFullShort(person: Person) {
    val (<!SYNTAX!><!>val x = <!UNRESOLVED_REFERENCE!>age<!><!SYNTAX!>) = person<!>
}

fun destructureThrowAlias(person: Person) {
    val (x = <!UNRESOLVED_REFERENCE!>age<!><!SYNTAX!><!> = 42<!SYNTAX!>) = person<!>
}

fun destructureUnderscoreFull(person: Person) {
    val (<!NAME_BASED_DESTRUCTURING_UNDERSCORE_WITHOUT_RENAMING!>_<!>) = person
}

fun destructureUnderscoreFull2(person: Person) {
    val (<!NAME_BASED_DESTRUCTURING_UNDERSCORE_WITHOUT_RENAMING!>_<!>, age) = person
}

fun destructureVarShortFull(person: Person) {
    var (<!SYNTAX!><!>var <!VARIABLE_WITH_NO_TYPE_NO_INITIALIZER!>age<!><!SYNTAX!>) = person<!>
}

fun destructureMixNewPositionalAndFullNamed(person: Person) {
    val (<!SYNTAX!>[<!> <!SYNTAX!>age<!><!SYNTAX!>], fullName = fullName) = person<!>
}

fun destructureMixNewPositionalAndFullNamed2(person: Person) {
    val (fullName = <!UNRESOLVED_REFERENCE!>y<!>, <!SYNTAX!>[<!><!SYNTAX!><!>val <!VARIABLE_WITH_NO_TYPE_NO_INITIALIZER!>age<!><!SYNTAX!>]) = person<!>
}

fun destructureMixNewPositionalAndShortNamed(person: Person) {
    val (<!SYNTAX!>[<!><!SYNTAX!><!>val <!VARIABLE_WITH_NO_TYPE_NO_INITIALIZER!>age<!><!SYNTAX!>], fullName ) = person<!>
}

fun destructureMixNewPositionalAndShortNamed2(person: Person) {
    var (<!SYNTAX!><!>var <!VARIABLE_WITH_NO_TYPE_NO_INITIALIZER!>fullName<!><!SYNTAX!>, [age]) = person<!>
}

fun destructureParenthesizedExpr(person: Person) {
    val (<!SYNTAX!>(<!><!SYNTAX!>age<!><!SYNTAX!>)) = person<!>
}

fun destructurePublic(person: Person) {
    <!WRONG_MODIFIER_TARGET!>public<!> val ( fullName = <!UNRESOLVED_REFERENCE!>y<!>, age ) = person
}

fun destructureConst(person: Person) {
    <!WRONG_MODIFIER_TARGET!>const<!> val ( fullName = <!UNRESOLVED_REFERENCE!>y<!>, age ) = person
}

fun destructurePublic1(person: Person) {
    <!WRONG_MODIFIER_TARGET!>public<!> val ( fullName = <!UNRESOLVED_REFERENCE!>y<!>, age ) = person
}

fun destructurePublic2(person: Person) {
    val (<!WRONG_MODIFIER_TARGET!>public<!> fullName, age ) = person
}

fun destructureConst1(person: Person) {
    <!WRONG_MODIFIER_TARGET!>const<!> val ( fullName, age ) = person
}

fun destructureConst2(person: Person) {
    val (<!WRONG_MODIFIER_TARGET!>const<!> fullName, <!WRONG_MODIFIER_TARGET!>const<!> age ) = person
}

fun destructureLateInit1(person: Person) {
    <!WRONG_MODIFIER_TARGET!>lateinit<!> var ( fullName, age ) = person
}

fun destructureLateInit2(person: Person) {
    var (lateinit fullName, lateinit age ) = person
}

/* GENERATED_FIR_TAGS: classDeclaration, collectionLiteral, data, destructuringDeclaration, functionDeclaration,
integerLiteral, localProperty, primaryConstructor, propertyDeclaration, unnamedLocalVariable */
