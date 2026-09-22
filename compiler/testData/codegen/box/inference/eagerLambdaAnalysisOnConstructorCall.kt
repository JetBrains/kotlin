// LANGUAGE: +EagerLambdaAnalysis, +CallCompletionRefinementsFor25, +UnitConversionsOnArbitraryExpressions, +InferThrowableTypeParameterToUpperBound
// IGNORE_BACKEND: JVM, JVM_IR

class Main {
   lateinit var result: String

   constructor(a: () -> Unit) {
       result = "not OK"
   }

   constructor(b: () -> String) {
       result = "OK"
   }
}

fun box(): String {
   return Main { "OK" }.result
}
