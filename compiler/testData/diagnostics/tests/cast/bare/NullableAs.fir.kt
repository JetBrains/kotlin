// !CHECK_TYPE

interface Tr<T>
interface G<T> : Tr<T>

fun test(tr: Tr<String>?) {
    val v = tr as G
    <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><G<String>>(v)
}