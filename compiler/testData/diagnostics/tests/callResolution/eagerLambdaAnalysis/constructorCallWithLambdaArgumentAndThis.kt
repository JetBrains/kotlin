// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +EagerLambdaAnalysis, +CallCompletionRefinementsFor25, +UnitConversionsOnArbitraryExpressions, +InferThrowableTypeParameterToUpperBound
// ISSUES: KT-86549

class Main(a: () -> Unit) {
   public var result: String = "OK"

   constructor(b: () -> String) : <!OVERLOAD_RESOLUTION_AMBIGUITY!>this<!>({ Unit }) {
       result = "not OK"
   }
}

open class Base(val a: () -> Unit) {
   constructor(b: () -> String) : this(a = {})
}

class Derived: Base {
   constructor() : <!OVERLOAD_RESOLUTION_AMBIGUITY!>super<!>({ Unit })
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, functionalType, lambdaLiteral, primaryConstructor,
propertyDeclaration, secondaryConstructor, stringLiteral */
