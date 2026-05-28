// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// WITH_STDLIB
// WITH_COROUTINES
// LANGUAGE: +DirectClassInheritors
import kotlin.coroutines.*

interface CoroutineTracerShim {
  companion object {
    var coroutineTracer: CoroutineTracerShim = object : CoroutineTracerShim {
      override fun rootTrace() = EmptyCoroutineContext
    }
  }

  fun rootTrace(): CoroutineContext
}

/* GENERATED_FIR_TAGS: additiveExpression, anonymousObjectExpression, assignment, checkNotNullCall, classDeclaration,
companionObject, functionDeclaration, functionalType, interfaceDeclaration, lambdaLiteral, localProperty, nullableType,
objectDeclaration, override, primaryConstructor, propertyDeclaration, safeCall, suspend, thisExpression, typeParameter,
typeWithExtension */
