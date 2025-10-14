// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +NameBasedDestructuring

data class Person(val fullName: String, val age: Int)

class Foo {
    val aProp: Int = 1
}

<!SYNTAX!>(val name = fullName)<!> = <!UNRESOLVED_REFERENCE!>person<!>

fun destructureEmptyShort(person: Person) {
    (val<!SYNTAX!><!>) = person
}


fun destructureEmptyRenameFull(person: Person) {
    (val<!SYNTAX!><!> = age) = person
}

fun destructureWithConst(person: Person) {
    (<!UNRESOLVED_REFERENCE!>const<!><!SYNTAX!><!> val <!VARIABLE_WITH_NO_TYPE_NO_INITIALIZER!>fullName<!><!SYNTAX!>) = person<!>
    (<!UNRESOLVED_REFERENCE!>const<!><!SYNTAX!><!> var <!VARIABLE_WITH_NO_TYPE_NO_INITIALIZER!>age<!><!SYNTAX!>) = person<!>
}

fun destructureWithPrivate(person: Person) {
    (<!UNRESOLVED_REFERENCE!>private<!><!SYNTAX!><!> val <!VARIABLE_WITH_NO_TYPE_NO_INITIALIZER!>fullName<!><!SYNTAX!>) = person<!>
    (<!UNRESOLVED_REFERENCE!>private<!><!SYNTAX!><!> var <!VARIABLE_WITH_NO_TYPE_NO_INITIALIZER!>age<!><!SYNTAX!>) = person<!>
}

fun destructureFullShort(person: Person) {
    val (<!SYNTAX!><!>val x = <!UNRESOLVED_REFERENCE!>age<!><!SYNTAX!>) = person<!>
}


fun destructureThrowAlias(person: Person) {
    (val x = <!UNRESOLVED_REFERENCE!>age<!><!SYNTAX!><!> = 42<!SYNTAX!>) = person<!>
}

fun destructureUnderscoreFull(person: Person) {
    (val <!NAME_BASED_DESTRUCTURING_UNDERSCORE_WITHOUT_RENAMING!>_<!>) = person
}

fun destructureUnderscoreFull2(person: Person) {
    (val <!NAME_BASED_DESTRUCTURING_UNDERSCORE_WITHOUT_RENAMING!>_<!>, val age) = person
}

fun destructureVarShortFull(person: Person) {
    var (<!SYNTAX!><!>var <!VARIABLE_WITH_NO_TYPE_NO_INITIALIZER!>age<!><!SYNTAX!>) = person<!>
}

fun destructureMixNewPositionalAndFullNamed(person: Person) {
    ([<!SYNTAX!><!>val <!VARIABLE_WITH_NO_TYPE_NO_INITIALIZER!>age<!><!SYNTAX!>], val y = fullName) = person<!>
}

fun destructureMixNewPositionalAndFullNamed2(person: Person) {
    (val fullName = <!UNRESOLVED_REFERENCE!>y<!>, <!SYNTAX!>[<!><!SYNTAX!><!SYNTAX!><!>val<!> <!SYNTAX!>age]) = person<!>
}

fun destructureMixNewPositionalAndShortNamed(person: Person) {
    val (<!SYNTAX!>[<!><!SYNTAX!><!>val <!VARIABLE_WITH_NO_TYPE_NO_INITIALIZER!>age<!><!SYNTAX!>], val fullName ) = person<!>
}

fun destructureMixNewPositionalAndShortNamed2(person: Person) {
    val (<!SYNTAX!><!>var <!VARIABLE_WITH_NO_TYPE_NO_INITIALIZER!>fullName<!><!SYNTAX!>, [val age]) = person<!>
}

fun destructureParenthesizedExpr(person: Person) {
    (val <!SYNTAX!>(<!><!SYNTAX!>age<!><!SYNTAX!>)) = person<!>
}

fun destructurePublic(person: Person) {
    <!WRONG_MODIFIER_TARGET!>public<!> ( val fullName = <!UNRESOLVED_REFERENCE!>y<!>, val age ) = person
}

fun destructureConst(person: Person) {
    <!WRONG_MODIFIER_TARGET!>const<!> ( val fullName = <!UNRESOLVED_REFERENCE!>y<!>, val age ) = person
}


fun destructurePublic1(person: Person) {
    <!WRONG_MODIFIER_TARGET!>public<!> ( val fullName = <!UNRESOLVED_REFERENCE!>y<!>, val age ) = person
}

fun destructurePublic2(person: Person) {
    (<!UNRESOLVED_REFERENCE!>public<!><!SYNTAX!><!> val <!VARIABLE_WITH_NO_TYPE_NO_INITIALIZER!>fullName<!><!SYNTAX!>, val age ) = person<!>
}

fun destructureConst1(person: Person) {
    <!WRONG_MODIFIER_TARGET!>const<!> ( val fullName, val age ) = person
}

fun destructureConst2(person: Person) {
    (<!UNRESOLVED_REFERENCE!>const<!><!SYNTAX!><!> val <!VARIABLE_WITH_NO_TYPE_NO_INITIALIZER!>fullName<!><!SYNTAX!>, const val age ) = person<!>
}

fun destructureLateInit1(person: Person) {
    <!WRONG_MODIFIER_TARGET!>lateinit<!> ( val fullName, val age ) = person
}

fun destructureLateInit2(person: Person) {
    (<!UNRESOLVED_REFERENCE!>lateinit<!><!SYNTAX!><!> val <!VARIABLE_WITH_NO_TYPE_NO_INITIALIZER!>fullName<!><!SYNTAX!>, lateinit val age ) = person<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, collectionLiteral, data, destructuringDeclaration, functionDeclaration,
integerLiteral, localProperty, primaryConstructor, propertyDeclaration, unnamedLocalVariable */
