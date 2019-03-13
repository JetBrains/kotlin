// !WITH_NEW_INFERENCE

fun test(mc: MutableCollection<out CharSequence>) {
    mc.addAll(<!NI;TYPE_MISMATCH, OI;TYPE_MISMATCH_DUE_TO_TYPE_PROJECTIONS!>mc<!>)

    mc.addAll(<!NI;TYPE_MISMATCH, OI;TYPE_MISMATCH_DUE_TO_TYPE_PROJECTIONS!>arrayListOf<CharSequence>()<!>)
    mc.addAll(<!OI;IMPLICIT_NOTHING_AS_TYPE_PARAMETER!>arrayListOf<!>())

    mc.addAll(<!NI;TYPE_MISMATCH, OI;TYPE_MISMATCH_DUE_TO_TYPE_PROJECTIONS!>listOf("")<!>)
    mc.addAll(<!NI;TYPE_MISMATCH, OI;TYPE_MISMATCH_DUE_TO_TYPE_PROJECTIONS!>listOf<String>("")<!>)
    mc.addAll(<!NI;TYPE_MISMATCH, NI;TYPE_MISMATCH, OI;TYPE_MISMATCH_DUE_TO_TYPE_PROJECTIONS!>listOf<CharSequence>("")<!>)

    mc.addAll(<!OI;IMPLICIT_NOTHING_AS_TYPE_PARAMETER!>emptyList<!>())
    mc.addAll(emptyList<Nothing>())
}
