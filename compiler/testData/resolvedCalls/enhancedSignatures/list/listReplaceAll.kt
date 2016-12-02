fun notNullValues(list: MutableList<String>) {
    // TODO: Fix platform types
    list.<caret>replaceAll { it.length.toString() }
}

fun nullableValues(list: MutableList<String?>) {
    // TODO: Fix platform types
    list.<caret>replaceAll { it?.run {  length.toString() } }
}