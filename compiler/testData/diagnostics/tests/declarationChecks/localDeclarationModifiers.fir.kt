// RUN_PIPELINE_TILL: FRONTEND
package d

class T {
    fun baz() = 1
}

fun foo() {
    <!WRONG_MODIFIER_TARGET!>public<!> val i = 11
    <!WRONG_MODIFIER_TARGET!>abstract<!> val <!VARIABLE_WITH_NO_TYPE_NO_INITIALIZER!>j<!>
    <!WRONG_MODIFIER_TARGET!>override<!> fun T.baz() = 2
    <!WRONG_MODIFIER_TARGET!>private<!> fun bar() = 2
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, integerLiteral, localFunction,
localProperty, override, propertyDeclaration */
