// !CHECK_TYPE

interface Tr
interface G<T>

fun test(tr: Tr?) {
    val v = tr as <!NO_TYPE_ARGUMENTS_ON_RHS!>G?<!>
    checkSubtype<G<*>>(<!ARGUMENT_TYPE_MISMATCH!>v!!<!>)
}
