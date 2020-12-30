// !WITH_NEW_INFERENCE

fun test(mc: MutableCollection<out CharSequence>) {
    mc.addAll(<!TYPE_MISMATCH{NI}, TYPE_MISMATCH_DUE_TO_TYPE_PROJECTIONS{OI}!>mc<!>)

    mc.addAll(<!TYPE_MISMATCH{NI}, TYPE_MISMATCH_DUE_TO_TYPE_PROJECTIONS{OI}!>arrayListOf<CharSequence>()<!>)
    mc.addAll(arrayListOf())

    mc.addAll(<!TYPE_MISMATCH{NI}, TYPE_MISMATCH_DUE_TO_TYPE_PROJECTIONS{OI}!>listOf("")<!>)
    mc.addAll(<!TYPE_MISMATCH{NI}, TYPE_MISMATCH_DUE_TO_TYPE_PROJECTIONS{OI}!>listOf<String>("")<!>)
    mc.addAll(<!TYPE_MISMATCH{NI}, TYPE_MISMATCH_DUE_TO_TYPE_PROJECTIONS{OI}!>listOf<CharSequence>("")<!>)

    mc.addAll(emptyList())
    mc.addAll(emptyList<Nothing>())
}
