// RUN_PIPELINE_TILL: BACKEND
// IDE_MODE

// FILE: test.kt
package test

enum class MyAnnotationTarget {
    X, Y
}

annotation class MyTarget(vararg val allowedTargets: MyAnnotationTarget)

// FILE: main.kt
import test.MyTarget
import test.MyAnnotationTarget

@MyTarget(<!DEBUG_INFO_CSR_MIGHT_BE_USED!>MyAnnotationTarget.X<!>, <!DEBUG_INFO_CSR_MIGHT_BE_USED!>MyAnnotationTarget.Y<!>)
fun foo1() {}

@MyTarget(*[<!DEBUG_INFO_CSR_MIGHT_BE_USED!>MyAnnotationTarget.X<!>, <!DEBUG_INFO_CSR_MIGHT_BE_USED!>MyAnnotationTarget.Y<!>])
fun foo2() {}

@MyTarget(*arrayOf(<!DEBUG_INFO_CSR_MIGHT_BE_USED!>MyAnnotationTarget.X<!>, <!DEBUG_INFO_CSR_MIGHT_BE_USED!>MyAnnotationTarget.Y<!>))
fun foo3() {}

/* GENERATED_FIR_TAGS: enumDeclaration, enumEntry, equalityExpression, functionDeclaration, ifExpression, smartcast,
stringLiteral, whenExpression, whenWithSubject */
