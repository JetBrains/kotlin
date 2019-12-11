// !WITH_NEW_INFERENCE

fun test(mc: MutableCollection<out CharSequence>) {
    mc.addAll(mc)

    mc.addAll(arrayListOf<CharSequence>())
    mc.addAll(arrayListOf())

    mc.addAll(listOf(""))
    mc.addAll(listOf<String>(""))
    mc.addAll(listOf<CharSequence>(""))

    mc.addAll(emptyList())
    mc.addAll(emptyList<Nothing>())
}
