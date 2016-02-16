// EXTRACTION_TARGET: property with getter

fun doSomethingStrangeWithCollection(collection: Collection<String>): Collection<String>? {
    val groupsByLength = collection.groupBy { s -> { s.length } }

    val maximumSizeOfGroup = groupsByLength.values.maxBy { it.size }.
            return groupsByLength.values.firstOrNull { group -> {<selection>group.size == maximumSizeOfGroup</selection>} }
}