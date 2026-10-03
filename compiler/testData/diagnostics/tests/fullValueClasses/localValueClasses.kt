// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +FullValueClasses
// WITH_STDLIB

open class Identity

fun local() {
    <!WRONG_MODIFIER_TARGET!>value<!> class Local(val x: Int)

    @JvmInline
    <!WRONG_MODIFIER_TARGET!>value<!> class LocalInline(val x: Int)

    class LocalClass {
        <!WRONG_MODIFIER_TARGET!>value<!> <!NESTED_CLASS_NOT_ALLOWED!>class Nested<!>(val x: Int)
        inner <!VALUE_CLASS_NOT_TOP_LEVEL!>value<!> class Inner(val x: Int)
    }

    object {
        <!WRONG_MODIFIER_TARGET!>value<!> <!NESTED_CLASS_NOT_ALLOWED!>class InAnonymousObject<!>(val x: Int)
    }

    <!WRONG_MODIFIER_TARGET!>value<!> class WithSupertypeAndState(val x: Int) : Identity() {
        val y = 1
    }

    val lambda = {
        <!WRONG_MODIFIER_TARGET!>value<!> class InLambda(val x: Int)
    }
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, classDeclaration, functionDeclaration, inner, integerLiteral,
lambdaLiteral, localClass, localProperty, nestedClass, primaryConstructor, propertyDeclaration, value */
