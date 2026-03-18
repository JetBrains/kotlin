// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-18134
// WITH_STDLIB

// KT-18134: Warning IMPLICIT_CAST_TO_ANY isn't reported if expression used in parameters

class A

fun <T> foo(t: T) {}

fun main(args: Array<String>) {
    val v = if (true) A() else 1       // Warning IS reported
    print("${if (true) A() else 1}")   // Warning IS reported

    foo(if (true) A() else 1)    // Warning is NOT reported (bug)
    print(if (true) A() else 1)  // Warning is NOT reported (bug)
    1 to if (true) A() else 1    // Warning is NOT reported (bug)
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, ifExpression, integerLiteral, localProperty, nullableType,
propertyDeclaration, typeParameter */
