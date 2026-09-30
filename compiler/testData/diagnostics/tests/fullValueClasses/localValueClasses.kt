// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +FullValueClasses
// WITH_STDLIB

open class Identity

fun local() {
    <!VALUE_CLASS_NOT_TOP_LEVEL, WRONG_MODIFIER_TARGET!>value<!> class Local(val x: Int)

    @JvmInline
    <!VALUE_CLASS_NOT_TOP_LEVEL, WRONG_MODIFIER_TARGET!>value<!> class LocalInline(val x: Int)

    class LocalClass {
        <!VALUE_CLASS_NOT_TOP_LEVEL, WRONG_MODIFIER_TARGET!>value<!> <!NESTED_CLASS_NOT_ALLOWED!>class Nested<!>(val x: Int)
        inner <!VALUE_CLASS_NOT_TOP_LEVEL!>value<!> class Inner(val x: Int)
    }

    object {
        <!VALUE_CLASS_NOT_TOP_LEVEL, WRONG_MODIFIER_TARGET!>value<!> <!NESTED_CLASS_NOT_ALLOWED!>class InAnonymousObject<!>(val x: Int)
    }

    <!VALUE_CLASS_NOT_TOP_LEVEL, WRONG_MODIFIER_TARGET!>value<!> class WithSupertypeAndState(val x: Int) : <!VALUE_CLASS_CANNOT_EXTEND_IDENTITY_CLASSES!>Identity<!>() {
        <!PROPERTY_WITH_BACKING_FIELD_INSIDE_VALUE_CLASS!>val y<!> = 1
    }

    val lambda = {
        <!VALUE_CLASS_NOT_TOP_LEVEL, WRONG_MODIFIER_TARGET!>value<!> class InLambda(val x: Int)
    }
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, classDeclaration, functionDeclaration, inner, integerLiteral,
lambdaLiteral, localClass, localProperty, nestedClass, primaryConstructor, propertyDeclaration, value */
