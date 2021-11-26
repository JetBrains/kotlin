// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_PARAMETER, -UNUSED_EXPRESSION, -UNUSED_VARIABLE
// !LANGUAGE: +InlineClasses -AllowResultInReturnType, -JvmInlineValueClasses

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

fun returnTypePublic(): Result<Int> = TODO()
internal fun returnTypeInternal(): Result<Int> = TODO()
private fun returnTypePrivate(): Result<Int> = TODO()
fun returnTypeNullable(): Result<Int>? = TODO()
fun returnTypeAlias(): ResultAlias<Int> = TODO()
fun returnInferred(r1: Result<Int>, r2: ResultAlias<Int>, b: Boolean) = if (b) r1 else r2

fun returnTypeInline(): InlineResult<Int> = TODO()
fun returnContainer(): List<Result<Int>> = TODO()

val topLevelP: Result<Int> = TODO()
val topLevelPInferred = topLevelP
internal val topLevelPInternal: Result<Int> = TODO()

private val topLevelPPrivate: Result<Int> = TODO()
private val topLevelPPrivateInferred = topLevelP

private val topLevelPPrivateCustomGetter: Result<Int>
    get() = TODO()

val asFunctional: () -> Result<Int> = TODO()

open class PublicCls(
    val r1: Result<String>,
    val r2: Result<Int>?,
    val r3: ResultAlias<Int>,
    val r4: ResultAlias<Int>?,

    val r5: InlineResult<Int>,

    internal val r6: Result<Int>,

    private val r7: Result<Int>,
    val r8: List<Result<Int>>
) {
    val p1: Result<Int> = TODO()
    var p2: Result<Int> = TODO()
    val p3: ResultAlias<Int>? = TODO()

    val p4 = p1

    internal val p5: Result<Int> = TODO()

    private var p6: Result<Int> = TODO()

    internal val p7 = p1
    protected val p8 = p1

    fun returnInCls(): Result<Int> = TODO()
    protected fun returnInClsProtected(): Result<Int> = TODO()
    private fun returnInClsPrivate(): Result<Int> = TODO()
}

internal open class InternalCls(
    val r1: Result<Int>,
    val r2: ResultAlias<Int>?,

    val r3: List<Result<Int>>
) {
    companion object {
        val cr1: Result<Int> = TODO()

        private val cr2: Result<Int> = TODO()
    }

    val p1 = r1
    val p2: Result<String> = TODO()

    protected val p3 = p1

    fun returnInInternal(): Result<Int> = TODO()
    protected fun returnInClsProtected(): Result<Int> = TODO()
}

private class PrivateCls(
    val r1: Result<Int>,
    val r2: ResultAlias<Int>?,
    val r3: List<Result<Int>>
) {
    companion object {
        val cr1: Result<Int> = TODO()
        private val cr2: Result<Int> = TODO()
    }

    val p1 = r1
    val p2: Result<String> = TODO()

    fun returnInPrivate(): Result<Int> = TODO()
}

fun local(r: Result<Int>) {
    val l1: Result<Int>? = null
    val l2 = r

    fun localFun(): Result<Int> = TODO()

    class F {
        val p1: Result<Int> = r
        val p2 = r
    }
}

fun <T> resultInGenericFun(r: Result<Int>): T = r <!UNCHECKED_CAST!>as T<!>

val asFunPublic: () -> Result<Int> = TODO()
private val asFun: () -> Result<Int>? = TODO()
