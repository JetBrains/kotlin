fun valuesNotNull(map: MutableMap<Int, String>) {
    map.<caret>replace(1, "x")
    map.<caret>replace(1, "x", "y")

    map.<caret>replaceAll { k, v -> "$k to ${v.length}" }
}

fun valuesNullable(map: MutableMap<Int, String?>) {
    map.<caret>replace(1, null)
    map.<caret>replace(1, null, "x")

    map.<caret>replaceAll { k, v -> "$k to $v" }
}
