// !CHECK_TYPE

interface Tr
interface G<T>

fun test(tr: Tr) {
    val v = tr as <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>G<!>
    <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><G<*>>(v)
}