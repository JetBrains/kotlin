// ISSUE: KT-37403
// WITH_STDLIB

// KT-37403: IR for FUNCTION_REFERENCE type is missing type argument annotations in new inference
fun bar(f: (Float) -> Float): Float {
    return f(1f)
}

fun foo(arg: Float): Float {
    return arg
}

val testRef2 = ::foo // type: KFunction1<@[ParameterName(name = 'arg')] Float, Float>
val testRef1 = bar(::foo) // IR type was KFunction1<Float, Float> (missing @ParameterName annotation)

/* GENERATED_FIR_TAGS: callableReference, functionDeclaration, functionalType, propertyDeclaration */
