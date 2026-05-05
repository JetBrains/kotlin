// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// FIR_DUMP
// LANGUAGE: +EagerLambdaAnalysis
class Inv<T>(val x: T)

fun <T> Inv<out T>.debounce(timeoutMillis: (T) -> Int): Inv<T> = TODO()

@JvmName("debounceDuration")
fun <T> Inv<out T>.debounce(timeout: (T) -> String): Inv<T> = TODO()

fun foo1(x: Inv<Int>) {}

fun foo2(x: Inv<Any>) {}

fun foo3(x: Inv<Any>) {}
fun foo3(x: Any) {}

fun test(x: Inv<Int>) {
    x.debounce { 0 }.x.div(1)

    val y0 = x.debounce { 1 }
    val y1: Inv<Int> = x.debounce { 2 }
    val y2: Inv<Any> <!INITIALIZER_TYPE_MISMATCH!>=<!> x.debounce { 3 }
    val y3: Inv<*> = x.debounce { 4 }
    val y4: Any = x.debounce { 5 }

    // Unlike to how @OverloadResolutionByLambdaReturnType worked, we don't allow to fix a type variable invariantly used
    // in return type of the call used in ContextDependent position (i.e., as an argument).
    foo1(x.<!OVERLOAD_RESOLUTION_AMBIGUITY!>debounce<!> { 6 })
    foo2(x.<!OVERLOAD_RESOLUTION_AMBIGUITY!>debounce<!> { 7 })
    foo3(x.<!OVERLOAD_RESOLUTION_AMBIGUITY!>debounce<!> { 8 })
}

/* GENERATED_FIR_TAGS: classDeclaration, classReference, funWithExtensionReceiver, functionDeclaration, functionalType,
integerLiteral, lambdaLiteral, localProperty, nullableType, outProjection, primaryConstructor, propertyDeclaration,
starProjection, stringLiteral, thisExpression, typeParameter */
