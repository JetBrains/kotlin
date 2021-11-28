fun test(mc: MutableCollection<out CharSequence>) {
    mc.addAll(<!TYPE_MISMATCH!>mc<!>)

    mc.addAll(<!TYPE_MISMATCH!>arrayListOf<CharSequence>()<!>)
    mc.addAll(arrayListOf())

    mc.addAll(<!TYPE_MISMATCH!>listOf("")<!>)
    mc.addAll(<!TYPE_MISMATCH!>listOf<String>("")<!>)
    mc.addAll(<!TYPE_MISMATCH!>listOf<CharSequence>("")<!>)

    mc.addAll(emptyList())
    mc.addAll(emptyList<Nothing>())
}
