// WITH_RUNTIME
// INTENTION_TEXT: "Replace with '+= filterIsInstance<>()'"

// decided to not generate "filterIsInstanceTo" because it either requires 2 type arguments (looks awful) or no type arguments at all (looks confusing)

fun foo(list: List<Any>, target: MutableCollection<String>) {
    <caret>for (o in list) {
        if (o is String) {
            target.add(o)
        }
    }
}