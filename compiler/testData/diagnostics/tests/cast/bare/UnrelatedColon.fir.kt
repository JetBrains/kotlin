// !CHECK_TYPE
// NI_EXPECTED_FILE

interface Tr
interface G<T>

fun test(tr: Tr) = <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><G>(tr)