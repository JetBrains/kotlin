// WITH_RUNTIME

public fun List<String>.fn() : List<String> {
    <caret>return map {
        if (it.isEmpty()) return emptyList()
        it
    }
}