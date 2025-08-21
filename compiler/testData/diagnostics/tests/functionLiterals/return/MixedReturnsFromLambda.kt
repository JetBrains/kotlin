// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// CHECK_TYPE

interface A
interface B: A
interface C: A


fun test(a: C, b: B) {
    val x = run f@{
      if (a != b) return@f a
      b
    }
    checkSubtype<A>(x)
}

/* GENERATED_FIR_TAGS: classDeclaration, equalityExpression, funWithExtensionReceiver, functionDeclaration,
functionalType, ifExpression, infix, interfaceDeclaration, lambdaLiteral, localProperty, nullableType,
propertyDeclaration, typeParameter, typeWithExtension */
