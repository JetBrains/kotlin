// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// LANGUAGE: +EagerLambdaAnalysis, +CallCompletionRefinementsFor25, +InferThrowableTypeParameterToUpperBound

open class Base {
   protected fun foo(x: () -> String) = 1
}

fun returnBase(): Base = Derived()

class Derived : Base() {
   fun foo(x: () -> Unit) = "(2)"

   fun test(x: Base) {
       val resultBeforeSmartcast = x.<!INVISIBLE_REFERENCE!>foo<!> { "OK" }

       if (x is Derived) {
           val result = x.foo { "OK" }
           <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>result<!>
       }

       var y: Base = returnBase()
       y.<!INVISIBLE_REFERENCE!>foo<!> { "OK" }
       if (y is Derived) {
           val result = y.foo { "OK" }
           <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>result<!>
       }
   }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, functionalType, ifExpression, integerLiteral, isExpression,
lambdaLiteral, localProperty, propertyDeclaration, smartcast, stringLiteral */
