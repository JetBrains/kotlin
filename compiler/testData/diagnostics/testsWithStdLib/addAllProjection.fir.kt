
fun test(mc: MutableCollection<out CharSequence>) {
    mc.addAll(<!ARGUMENT_TYPE_MISMATCH!>mc<!>)

    mc.addAll(<!ARGUMENT_TYPE_MISMATCH!>arrayListOf<CharSequence>()<!>)
    mc.addAll(arrayListOf())

    mc.addAll(<!ARGUMENT_TYPE_MISMATCH!>listOf("")<!>)
    mc.addAll(<!ARGUMENT_TYPE_MISMATCH!>listOf<String>("")<!>)
    mc.addAll(<!ARGUMENT_TYPE_MISMATCH!>listOf<CharSequence>("")<!>)

    mc.addAll(emptyList())
    mc.addAll(emptyList<Nothing>())
}
