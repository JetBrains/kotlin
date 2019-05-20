// !WITH_NEW_INFERENCE

fun test(mc: MutableCollection<out CharSequence>) {
    mc.addAll(<!TYPE_MISMATCH_DUE_TO_TYPE_PROJECTIONS!>mc<!>)

    mc.addAll(<!TYPE_MISMATCH_DUE_TO_TYPE_PROJECTIONS!>arrayListOf<CharSequence>()<!>)
    mc.addAll(arrayListOf())

    mc.addAll(<!TYPE_MISMATCH_DUE_TO_TYPE_PROJECTIONS!>listOf("")<!>)
    mc.addAll(<!TYPE_MISMATCH_DUE_TO_TYPE_PROJECTIONS!>listOf<String>("")<!>)
    mc.addAll(<!NI;TYPE_MISMATCH, TYPE_MISMATCH_DUE_TO_TYPE_PROJECTIONS!>listOf<CharSequence>("")<!>)

    mc.addAll(emptyList())
    mc.addAll(emptyList<Nothing>())
}
