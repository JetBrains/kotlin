// !CHECK_TYPE

interface Tr
interface G<T>

fun test(tr: Tr) = checkSubtype<<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>G<!>>(tr)