// RUN_PIPELINE_TILL: FRONTEND
// KT-14469: SOE during effective visibility evaluation

abstract class Base(private val v: String)

fun bar(arg: String) = arg

class Derived : Base("123") {

    private <!NOTHING_TO_INLINE!>inline<!> fun foo() {
        bar(<!INVISIBLE_MEMBER!>v<!>)
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, inline, primaryConstructor, propertyDeclaration,
stringLiteral */
