// "Create function" "false"
// ACTION: Create local variable 'maximumSizeOfGroup'
// ACTION: Create object 'maximumSizeOfGroup'
// ACTION: Create parameter 'maximumSizeOfGroup'
// ACTION: Create property 'maximumSizeOfGroup'
// ACTION: Introduce local variable
// ACTION: Rename reference
// ERROR: A 'return' expression required in a function with a block body ('{...}')
// ERROR: Cannot infer a type for this parameter. Please specify it explicitly.
// ERROR: Cannot infer a type for this parameter. Please specify it explicitly.
// ERROR: Expression 'return groupsByLength.values.firstOrNull { group -> {group.size == maximumSizeOfGroup} }' cannot be a selector (occur after a dot)
// ERROR: Unresolved reference: groupBy
// ERROR: Unresolved reference: it
// ERROR: Unresolved reference: maximumSizeOfGroup

fun doSomethingStrangeWithCollection(collection: Collection<String>): Collection<String>? {
    val groupsByLength = collection.groupBy { s -> { s.length } }

    val maximumSizeOfGroup = groupsByLength.values.maxBy { it.size }.
    return groupsByLength.values.firstOrNull { group -> {group.size == <caret>maximumSizeOfGroup} }
}
