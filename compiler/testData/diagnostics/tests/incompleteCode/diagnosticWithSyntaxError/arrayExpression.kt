// RUN_PIPELINE_TILL: FRONTEND
package bar

fun main() {
    class Some

    Some[<!SYNTAX!><!>] names <!SYNTAX!>=<!> <!NO_GET_METHOD!>["ads"]<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, localClass, stringLiteral */
