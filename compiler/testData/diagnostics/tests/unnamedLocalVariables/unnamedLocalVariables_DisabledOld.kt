// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-74809
// WITH_STDLIB
// LANGUAGE: -UnnamedLocalVariables, +NameBasedDestructuring -NameBasedDestructuring -DeprecateNameMismatchInShortDestructuringWithParentheses -EnableNameBasedDestructuringShortForm

fun writeTo(): Boolean = false

fun foo() {
    val <!UNDERSCORE_IS_RESERVED!>_<!> = writeTo()
    val (a, _) = 1 to 2
    val (_) = 'a' to 'b'

    <!UNSUPPORTED!>(val f = first, val _ = second) = "first" to "second"<!>

    when(val <!UNDERSCORE_IS_RESERVED!>_<!> = writeTo()) {
        true -> {}
        false -> {}
    }

    for (<!UNDERSCORE_IS_RESERVED!>_<!> in 1..10) {}

    val <!UNDERSCORE_IS_RESERVED!>_<!> = object {
        val <!UNDERSCORE_IS_RESERVED!>_<!> = <!UNRESOLVED_REFERENCE!>call<!>()
    }

    val <!UNDERSCORE_IS_RESERVED!>_<!> by lazy { 10 }
    var <!UNDERSCORE_IS_RESERVED!>_<!> = writeTo()

    val <!UNDERSCORE_IS_RESERVED, VARIABLE_WITH_NO_TYPE_NO_INITIALIZER!>_<!>
    val <!UNDERSCORE_IS_RESERVED!>_<!>: Int
    val <!UNDERSCORE_IS_RESERVED!>_<!>: String = <!CONSTANT_EXPECTED_TYPE_MISMATCH!>1<!>
    val <!UNDERSCORE_IS_RESERVED!>_<!> = 1
    val <!UNDERSCORE_IS_RESERVED!>_<!>: Int = 1
}

class Foo() {
    val <!UNDERSCORE_IS_RESERVED!>_<!> = <!UNRESOLVED_REFERENCE!>initMe<!>()
}

class Foo2() {
    init {
        val <!UNDERSCORE_IS_RESERVED!>_<!> = <!UNRESOLVED_REFERENCE!>initMe<!>()
    }
}

val <!UNDERSCORE_IS_RESERVED!>_<!> = writeTo()

val Int.<!UNDERSCORE_IS_RESERVED!>_<!>: String
    get() = this.toString()

val <T> T.<!UNDERSCORE_IS_RESERVED!>_<!>: String
    get() = this.toString()

/* GENERATED_FIR_TAGS: anonymousObjectExpression, classDeclaration, destructuringDeclaration, equalityExpression,
forLoop, functionDeclaration, getter, init, integerLiteral, lambdaLiteral, localProperty, nullableType,
primaryConstructor, propertyDeclaration, propertyDelegate, propertyWithExtensionReceiver, rangeExpression, smartcast,
thisExpression, typeParameter, unnamedLocalVariable, whenExpression, whenWithSubject */
