// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE -UNNECESSARY_NOT_NULL_ASSERTION -UNNECESSARY_SAFE_CALL

fun getFun(s: (p: Int) -> Unit): Function1<Int, Int> = {11}

inline fun getInlineFun(s: (p: Int) -> Unit): Function1<Int, Int> = {11}

inline fun testExtension(s: (p: Int) -> Unit) {
    getFun(<!USAGE_IS_NOT_INLINABLE!>s<!>).invoke(10)
    getInlineFun(s).invoke(10)
    getInlineFun(s)!!.invoke(10)
    getInlineFun(s)?.invoke(10)
}
