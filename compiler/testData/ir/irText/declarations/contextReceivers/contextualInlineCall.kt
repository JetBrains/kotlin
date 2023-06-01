// !LANGUAGE: +ContextReceivers

class Context {
    fun c() = 1
}

context(Context)
inline fun testInline() = c()

context(Context)
inline fun testInlineWithArg(i: Int) = i + c()

context(Context)
inline fun Int.testInlineWithExtensionAndArg(i: Int) = this@Int + i + c()

context(Context)
inline fun Int.testInlineWithExtensionAndMultipleArgs(i1: Int, i2: Int) = this@Int + i1 + i2 + c()

class A(val a: Any?)

context(Context, A)
inline fun Int.testInlineWithExtensionAndMultipleContextsAndArgs(i1: Int = 1, i2: Int = 2) =
    this@Int + i1 + i2 + c() + if (this@A.a == null) 0 else 1

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
