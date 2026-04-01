// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-6328

// KT-6328: Lookup of short form `invoke()` operator function doesn't work on generic properties
val <T> T.self: T
    get() = this

class F<T> {
    operator fun invoke() {}
}

fun fn1() = F<String>().self.invoke() // ok
fun fn2() = F<String>().self() // was [FUNCTION_EXPECTED] in K1, should be ok
fun fn3() = F<String>()() // ok

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, getter, nullableType, operator, propertyDeclaration,
propertyWithExtensionReceiver, thisExpression, typeParameter */
