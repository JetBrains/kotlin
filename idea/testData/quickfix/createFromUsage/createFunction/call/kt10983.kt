// "Create function" "false"
// ERROR: A 'return' expression required in a function with a block body ('{...}')
// ERROR: The expression cannot be a selector (occur after a dot)
// ERROR: Type inference failed: inline fun <T> Iterable<T>.firstOrNull(predicate: (T) -> Boolean): T?<br>cannot be applied to<br>receiver: Collection<List<String>>  arguments: ((List<String>) -> () -> Boolean)<br>
// ERROR: Type mismatch: inferred type is (List<String>) -> () -> Boolean but (List<String>) -> Boolean was expected
// ERROR: Unresolved reference: maximumSizeOfGroup

fun doSomethingStrangeWithCollection(collection: Collection<String>): Collection<String>? {
    val groupsByLength = collection.groupBy { s -> { s.length } }

    val maximumSizeOfGroup = groupsByLength.values.maxBy { it.size }.
    return groupsByLength.values.firstOrNull { group -> {group.size == maximumSizeOfGroup} }
}
