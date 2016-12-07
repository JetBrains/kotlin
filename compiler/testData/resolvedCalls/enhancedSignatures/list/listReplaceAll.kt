fun notNullValues(list: MutableList<String>) {
    list.<caret>replaceAll { it.length.toString() }
}

fun nullableValues(list: MutableList<String?>) {
    list.<caret>replaceAll { it?.run {  length.toString() } }
}