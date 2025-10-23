// LANGUAGE: +ContextParameters
// IGNORE_BACKEND_K1: ANY
// KT-51271

class Context {
    fun c() = 1
}

context(context: Context)
inline fun testInline() = context.c()

context(context: Context)
inline fun testInlineWithArg(i: Int) = i + context.c()

context(context: Context)
inline fun Int.testInlineWithExtensionAndArg(i: Int) = this@testInlineWithExtensionAndArg + i + context.c()

context(context: Context)
inline fun Int.testInlineWithExtensionAndMultipleArgs(i1: Int, i2: Int) = this@testInlineWithExtensionAndMultipleArgs + i1 + i2 + context.c()

class A(val a: Any?)

context(context: Context, a: A)
inline fun Int.testInlineWithExtensionAndMultipleContextsAndArgs(i1: Int = 1, i2: Int = 2) =
    this@testInlineWithExtensionAndMultipleContextsAndArgs + i1 + i2 + context.c() + if (a.a == null) 0 else 1

fun box(): String = with(Context()) {
    var result = 0
    result += testInline()
    result += testInlineWithArg(1)
    result += 1.testInlineWithExtensionAndArg(1)
    result += 1.testInlineWithExtensionAndMultipleArgs(1, 2)
    with(A(1)) {
        result += 1.testInlineWithExtensionAndMultipleContextsAndArgs(1, 2)
        result += 1.testInlineWithExtensionAndMultipleContextsAndArgs()
    }
    return if (result == 23) "OK" else "fail"
}
