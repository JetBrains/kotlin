// FIR_IDENTICAL
// ISSUE: KT-57001

interface ConverterFromString<T> {

    fun ofS(s: String): T

    fun nullable(nullText: String): ConverterFromString<T?> = object: ConverterFromString<T?> {
        override fun ofS(s: String): T? = if (s == nullText) null else this@ConverterFromString.ofS(s)
    }
}
