// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// CHECK_TYPE

fun test() {
    val x = run f@{
      run ff@ {
        return@ff "2"
      }
      return@f 1
    }
    checkSubtype<Int>(x)
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, functionalType, infix,
integerLiteral, lambdaLiteral, localProperty, nullableType, propertyDeclaration, stringLiteral, typeParameter,
typeWithExtension */
