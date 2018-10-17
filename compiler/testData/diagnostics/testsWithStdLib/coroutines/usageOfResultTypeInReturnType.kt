// !DIAGNOSTICS: -UNUSED_PARAMETER, -UNUSED_EXPRESSION, -UNUSED_VARIABLE
// !LANGUAGE: +InlineClasses

typealias ResultAlias<T> = Result<T>

inline class InlineResult<out T>(private val r: Result<T>)

fun params(
    r1: Result<Int>,
    r2: Result<Int>?,
    r3: ResultAlias<String>,
    r4: List<Result<Int>>,
    r5: InlineResult<Int>,
    <!FORBIDDEN_VARARG_PARAMETER_TYPE!>vararg<!> r6: Result<Int>
) {}

class CtorParams(r1: Result<Int>)

fun returnTypePublic(): <!RESULT_CLASS_IN_RETURN_TYPE!>Result<Int><!> = TODO()
internal fun returnTypeInternal(): <!RESULT_CLASS_IN_RETURN_TYPE!>Result<Int><!> = TODO()
private fun returnTypePrivate(): <!RESULT_CLASS_IN_RETURN_TYPE!>Result<Int><!> = TODO()
fun returnTypeNullable(): <!RESULT_CLASS_IN_RETURN_TYPE!>Result<Int>?<!> = TODO()
fun returnTypeAlias(): <!RESULT_CLASS_IN_RETURN_TYPE!>ResultAlias<Int><!> = TODO()
fun <!RESULT_CLASS_IN_RETURN_TYPE!>returnInferred<!>(r1: Result<Int>, r2: ResultAlias<Int>, b: Boolean) = if (b) r1 else r2

fun returnTypeInline(): InlineResult<Int> = TODO()
fun returnContainer(): List<Result<Int>> = TODO()

val topLevelP: <!RESULT_CLASS_IN_RETURN_TYPE!>Result<Int><!> = TODO()
val <!RESULT_CLASS_IN_RETURN_TYPE!>topLevelPInferred<!> = topLevelP
internal val topLevelPInternal: <!RESULT_CLASS_IN_RETURN_TYPE!>Result<Int><!> = TODO()

private val topLevelPPrivate: Result<Int> = TODO()
private val topLevelPPrivateInferred = topLevelP

private val topLevelPPrivateCustomGetter: <!RESULT_CLASS_IN_RETURN_TYPE!>Result<Int><!>
    get() = TODO()

val asFunctional: () -> Result<Int> = TODO()

open class PublicCls(
    val r1: <!RESULT_CLASS_IN_RETURN_TYPE!>Result<String><!>,
    val r2: <!RESULT_CLASS_IN_RETURN_TYPE!>Result<Int>?<!>,
    val r3: <!RESULT_CLASS_IN_RETURN_TYPE!>ResultAlias<Int><!>,
    val r4: <!RESULT_CLASS_IN_RETURN_TYPE!>ResultAlias<Int>?<!>,

    val r5: InlineResult<Int>,

    internal val r6: <!RESULT_CLASS_IN_RETURN_TYPE!>Result<Int><!>,

    private val r7: Result<Int>,
    val r8: List<Result<Int>>
) {
    val p1: <!RESULT_CLASS_IN_RETURN_TYPE!>Result<Int><!> = TODO()
    var p2: <!RESULT_CLASS_IN_RETURN_TYPE!>Result<Int><!> = TODO()
    val p3: <!RESULT_CLASS_IN_RETURN_TYPE!>ResultAlias<Int>?<!> = TODO()

    val <!RESULT_CLASS_IN_RETURN_TYPE!>p4<!> = p1

    internal val p5: <!RESULT_CLASS_IN_RETURN_TYPE!>Result<Int><!> = TODO()

    private var p6: Result<Int> = TODO()

    internal val <!RESULT_CLASS_IN_RETURN_TYPE!>p7<!> = p1
    protected val <!RESULT_CLASS_IN_RETURN_TYPE!>p8<!> = p1

    fun returnInCls(): <!RESULT_CLASS_IN_RETURN_TYPE!>Result<Int><!> = TODO()
    protected fun returnInClsProtected(): <!RESULT_CLASS_IN_RETURN_TYPE!>Result<Int><!> = TODO()
    private fun returnInClsPrivate(): <!RESULT_CLASS_IN_RETURN_TYPE!>Result<Int><!> = TODO()
}

internal open class InternalCls(
    val r1: <!RESULT_CLASS_IN_RETURN_TYPE!>Result<Int><!>,
    val r2: <!RESULT_CLASS_IN_RETURN_TYPE!>ResultAlias<Int>?<!>,

    val r3: List<Result<Int>>
) {
    companion object {
        val cr1: <!RESULT_CLASS_IN_RETURN_TYPE!>Result<Int><!> = TODO()

        private val cr2: Result<Int> = TODO()
    }

    val <!RESULT_CLASS_IN_RETURN_TYPE!>p1<!> = r1
    val p2: <!RESULT_CLASS_IN_RETURN_TYPE!>Result<String><!> = TODO()

    protected val <!RESULT_CLASS_IN_RETURN_TYPE!>p3<!> = p1

    fun returnInInternal(): <!RESULT_CLASS_IN_RETURN_TYPE!>Result<Int><!> = TODO()
    protected fun returnInClsProtected(): <!RESULT_CLASS_IN_RETURN_TYPE!>Result<Int><!> = TODO()
}

private class PrivateCls(
    val r1: <!RESULT_CLASS_IN_RETURN_TYPE!>Result<Int><!>,
    val r2: <!RESULT_CLASS_IN_RETURN_TYPE!>ResultAlias<Int>?<!>,
    val r3: List<Result<Int>>
) {
    companion object {
        val cr1: <!RESULT_CLASS_IN_RETURN_TYPE!>Result<Int><!> = TODO()
        private val cr2: Result<Int> = TODO()
    }

    val <!RESULT_CLASS_IN_RETURN_TYPE!>p1<!> = r1
    val p2: <!RESULT_CLASS_IN_RETURN_TYPE!>Result<String><!> = TODO()

    fun returnInPrivate(): <!RESULT_CLASS_IN_RETURN_TYPE!>Result<Int><!> = TODO()
}

fun local(r: Result<Int>) {
    val l1: Result<Int>? = null
    val l2 = r

    fun localFun(): <!RESULT_CLASS_IN_RETURN_TYPE!>Result<Int><!> = TODO()

    class F {
        val p1: <!RESULT_CLASS_IN_RETURN_TYPE!>Result<Int><!> = r
        val <!RESULT_CLASS_IN_RETURN_TYPE!>p2<!> = r
    }
}

fun <T> resultInGenericFun(r: Result<Int>): T = r <!UNCHECKED_CAST!>as T<!>

val asFunPublic: () -> Result<Int> = TODO()
private val asFun: () -> Result<Int>? = TODO()
