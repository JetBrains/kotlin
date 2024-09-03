// IGNORE_INLINER: IR
fun foo1(a:Int, vararg b: String, c: Int = 0): Int = 0
fun Int.foo2(vararg b: String, c: Int = 0): Int  = 0

inline fun inlineFoo1(a:Int, vararg b: String, c: Int = 0) = 0
inline fun Int.inlineFoo2(vararg b: String, c: Int = 0) = 0

inline fun <reified T> inlineReifiedFoo1(a: T, vararg b: String, c: Int = 0) = 0
inline fun <reified T> T.inlineReifiedFoo2(vararg b: String, c: Int = 0) = 0

fun unitConversion(ref: Int.(Array<String>, Int) -> Unit) = ref
fun unitAndReceiverConversion(ref: (Int, Array<String>, Int) -> Unit) = ref
fun suspendAndUnitConversion(ref: suspend Int.(Array<String>, Int) -> Unit) = ref
fun suspendUnitAndReceiverConversion(ref: suspend (Int, Array<String>, Int) -> Unit) = ref
fun suspendAndDefaultConversion(ref: suspend Int.(Array<String>) -> Int) = ref
fun suspendDefaultAndReceiverConversion(ref: suspend (Int, Array<String>) -> Int) = ref
fun suspendAndVarargConversion(ref: suspend Int.(String, Int) -> Int) = ref
fun suspendVarargAndReceiverConversion(ref: suspend (Int, String, Int) -> Int) = ref
fun unitAndDefaultConversion(ref: Int.(Array<String>) -> Unit) = ref
fun unitDefaultAndReceiverConversion(ref: (Int, Array<String>) -> Unit) = ref
fun unitAndVarargConversion(ref: Int.(String, Int) -> Unit) = ref
fun unitVarargAndReceiverConversion(ref: (Int, String, Int) -> Unit) = ref
fun defaultAndVarargConversion(ref: Int.(String) -> Int) = ref
fun defaultVarargAndReceiverConversion(ref: (Int, String) -> Int) = ref
fun allExceptSuspendConversion(ref: Int.(String) -> Unit) = ref
fun allExceptSuspendAndReceiverConversion(ref: (Int, String) -> Unit) = ref
fun allExceptUnitConversion(ref: suspend Int.(String) -> Int) = ref
fun allExceptUnitAndReceiverConversion(ref: suspend (Int, String) -> Int) = ref
fun allExceptVarargConversion(ref: suspend Int.(Array<String>) -> Unit) = ref
fun allExceptVarargAndReceiverConversion(ref: suspend (Int, Array<String>) -> Unit) = ref
fun allExceptDefaultConversion(ref: suspend Int.(String, Int) -> Unit) = ref
fun allExceptDefaultAndReceiverConversion(ref: suspend (Int, String, Int) -> Unit) = ref
fun allConversions(ref: suspend Int.(String) -> Unit) = ref
fun allConversionsAndReceiver(ref: suspend (Int, String) -> Unit) = ref

fun testNormal(){
    unitConversion(::foo1)
    unitConversion(Int::foo2)
    unitAndReceiverConversion(Int::foo2)
    suspendAndUnitConversion(::foo1)
    suspendAndUnitConversion(Int::foo2)
    suspendUnitAndReceiverConversion(Int::foo2)
    suspendAndDefaultConversion(::foo1)
    suspendAndDefaultConversion(Int::foo2)
    suspendDefaultAndReceiverConversion(Int::foo2)
    suspendAndVarargConversion(::foo1)
    suspendAndVarargConversion(Int::foo2)
    suspendVarargAndReceiverConversion(Int::foo2)
    unitAndDefaultConversion(::foo1)
    unitAndDefaultConversion(Int::foo2)
    unitDefaultAndReceiverConversion(Int::foo2)
    unitAndVarargConversion(::foo1)
    unitAndVarargConversion(Int::foo2)
    unitVarargAndReceiverConversion(Int::foo2)
    defaultAndVarargConversion(::foo1)
    defaultAndVarargConversion(Int::foo2)
    defaultVarargAndReceiverConversion(Int::foo2)
    allExceptSuspendConversion(::foo1)
    allExceptSuspendConversion(Int::foo2)
    allExceptSuspendAndReceiverConversion(Int::foo2)
    allExceptUnitConversion(::foo1)
    allExceptUnitConversion(Int::foo2)
    allExceptUnitAndReceiverConversion(Int::foo2)
    allExceptVarargConversion(::foo1)
    allExceptVarargConversion(Int::foo2)
    allExceptVarargAndReceiverConversion(Int::foo2)
    allExceptDefaultConversion(::foo1)
    allExceptDefaultConversion(Int::foo2)
    allExceptDefaultAndReceiverConversion(Int::foo2)
    allConversions(::foo1)
    allConversions(Int::foo2)
    allConversionsAndReceiver(Int::foo2)
}

fun testInline(){
    unitConversion(::inlineFoo1)
    unitConversion(Int::inlineFoo2)
    unitAndReceiverConversion(Int::inlineFoo2)
    suspendAndUnitConversion(::inlineFoo1)
    suspendAndUnitConversion(Int::inlineFoo2)
    suspendUnitAndReceiverConversion(Int::inlineFoo2)
    suspendAndDefaultConversion(::inlineFoo1)
    suspendAndDefaultConversion(Int::inlineFoo2)
    suspendDefaultAndReceiverConversion(Int::inlineFoo2)
    suspendAndVarargConversion(::inlineFoo1)
    suspendAndVarargConversion(Int::inlineFoo2)
    suspendVarargAndReceiverConversion(Int::inlineFoo2)
    unitAndDefaultConversion(::inlineFoo1)
    unitAndDefaultConversion(Int::inlineFoo2)
    unitDefaultAndReceiverConversion(Int::inlineFoo2)
    unitAndVarargConversion(::inlineFoo1)
    unitAndVarargConversion(Int::inlineFoo2)
    unitVarargAndReceiverConversion(Int::inlineFoo2)
    defaultAndVarargConversion(::inlineFoo1)
    defaultAndVarargConversion(Int::inlineFoo2)
    defaultVarargAndReceiverConversion(Int::inlineFoo2)
    allExceptSuspendConversion(::inlineFoo1)
    allExceptSuspendConversion(Int::inlineFoo2)
    allExceptSuspendAndReceiverConversion(Int::inlineFoo2)
    allExceptUnitConversion(::inlineFoo1)
    allExceptUnitConversion(Int::inlineFoo2)
    allExceptUnitAndReceiverConversion(Int::inlineFoo2)
    allExceptVarargConversion(::inlineFoo1)
    allExceptVarargConversion(Int::inlineFoo2)
    allExceptVarargAndReceiverConversion(Int::inlineFoo2)
    allExceptDefaultConversion(::inlineFoo1)
    allExceptDefaultConversion(Int::inlineFoo2)
    allExceptDefaultAndReceiverConversion(Int::inlineFoo2)
    allConversions(::inlineFoo1)
    allConversions(Int::inlineFoo2)
    allConversionsAndReceiver(Int::inlineFoo2)
}

fun testInlineReifined() {
    unitConversion(::inlineReifiedFoo1)
    unitConversion(Int::inlineReifiedFoo2)
    unitAndReceiverConversion(Int::inlineReifiedFoo2)
    suspendAndUnitConversion(::inlineReifiedFoo1)
    suspendAndUnitConversion(Int::inlineReifiedFoo2)
    suspendUnitAndReceiverConversion(Int::inlineReifiedFoo2)
    suspendAndDefaultConversion(::inlineReifiedFoo1)
    suspendAndDefaultConversion(Int::inlineReifiedFoo2)
    suspendDefaultAndReceiverConversion(Int::inlineReifiedFoo2)
    suspendAndVarargConversion(::inlineReifiedFoo1)
    suspendAndVarargConversion(Int::inlineReifiedFoo2)
    suspendVarargAndReceiverConversion(Int::inlineReifiedFoo2)
    unitAndDefaultConversion(::inlineReifiedFoo1)
    unitAndDefaultConversion(Int::inlineReifiedFoo2)
    unitDefaultAndReceiverConversion(Int::inlineReifiedFoo2)
    unitAndVarargConversion(::inlineReifiedFoo1)
    unitAndVarargConversion(Int::inlineReifiedFoo2)
    unitVarargAndReceiverConversion(Int::inlineReifiedFoo2)
    defaultAndVarargConversion(::inlineReifiedFoo1)
    defaultAndVarargConversion(Int::inlineReifiedFoo2)
    defaultVarargAndReceiverConversion(Int::inlineReifiedFoo2)
    allExceptSuspendConversion(::inlineReifiedFoo1)
    allExceptSuspendConversion(Int::inlineReifiedFoo2)
    allExceptSuspendAndReceiverConversion(Int::inlineReifiedFoo2)
    allExceptUnitConversion(::inlineReifiedFoo1)
    allExceptUnitConversion(Int::inlineReifiedFoo2)
    allExceptUnitAndReceiverConversion(Int::inlineReifiedFoo2)
    allExceptVarargConversion(::inlineReifiedFoo1)
    allExceptVarargConversion(Int::inlineReifiedFoo2)
    allExceptVarargAndReceiverConversion(Int::inlineReifiedFoo2)
    allExceptDefaultConversion(::inlineReifiedFoo1)
    allExceptDefaultConversion(Int::inlineReifiedFoo2)
    allExceptDefaultAndReceiverConversion(Int::inlineReifiedFoo2)
    allConversions(::inlineReifiedFoo1)
    allConversions(Int::inlineReifiedFoo2)
    allConversionsAndReceiver(Int::inlineReifiedFoo2)
}
fun box(): String {
    testNormal()
    testInline()
    testInlineReifined()
    return "OK"
}