// !WITH_NEW_INFERENCE

fun test(mc: MutableCollection<out CharSequence>) {
    mc.addAll(mc)

    mc.<!INAPPLICABLE_CANDIDATE!>addAll<!>(arrayListOf<CharSequence>())
    mc.addAll(arrayListOf())

    mc.<!INAPPLICABLE_CANDIDATE!>addAll<!>(listOf(""))
    mc.<!INAPPLICABLE_CANDIDATE!>addAll<!>(listOf<String>(""))
    mc.<!INAPPLICABLE_CANDIDATE!>addAll<!>(listOf<CharSequence>(""))

    mc.addAll(emptyList())
    mc.addAll(emptyList<Nothing>())
}
