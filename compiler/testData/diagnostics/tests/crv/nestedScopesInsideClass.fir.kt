// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// ISSUE: KT-76776

@MustUseReturnValue
class A {
    companion object {
        fun foo(): Int = 123
    }

    class Nested {
        fun bar(): Int = 123
    }
}

fun test() {
    A.<!RETURN_VALUE_NOT_USED!>foo<!>()                 //unused
    A.Nested().<!RETURN_VALUE_NOT_USED!>bar<!>()        //unused
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, integerLiteral, nestedClass,
objectDeclaration */
