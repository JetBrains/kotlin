fun test(map: Map<String?, List<String>>) {
    val sortedMap = map.toSortedMap(nullsLast())
}
