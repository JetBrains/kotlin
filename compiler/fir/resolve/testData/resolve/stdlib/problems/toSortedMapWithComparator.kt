fun test(map: Map<String?, List<String>>) {
    val sortedMap = map.<!INAPPLICABLE_CANDIDATE!>toSortedMap<!>(nullsLast())
}
