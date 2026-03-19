// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-41337
// FULL_JDK

// KT-41337: Overload resolution ambiguity with generic types from java class with multiple type bounds
import java.util.function.Function

abstract class A<T> : Function<Int, T> where T: CharSequence, T: Comparable<T> {
    fun usage() {
        val a = apply(10)
        if (<!SENSELESS_COMPARISON!>a == null<!>) { // should not be an error
        }
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, equalityExpression, functionDeclaration, ifExpression, integerLiteral,
localProperty, propertyDeclaration, typeConstraint, typeParameter */
