// !CHECK_TYPE

trait Tr
trait G<T>

fun test(tr: Tr) = checkSubtype<<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>G<!>>(tr)