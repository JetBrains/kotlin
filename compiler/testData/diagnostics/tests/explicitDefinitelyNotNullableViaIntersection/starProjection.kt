// ISSUE: KT-58751
class Result<out T>

interface Convert<T> {
    fun convert(str: String): Result<T & Any>
}

fun Convert<*>.cnv(value: String): Result<Any> = <!TYPE_MISMATCH!>convert(value)<!>
