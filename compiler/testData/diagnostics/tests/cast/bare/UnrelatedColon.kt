// !CHECK_TYPE
// NI_EXPECTED_FILE

interface Tr
interface G<T>

fun test(tr: Tr) = checkSubtype<<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>G<!>>(tr)