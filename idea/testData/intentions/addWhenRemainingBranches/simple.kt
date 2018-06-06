// WITH_RUNTIME

enum class Entry {
    FOO, BAR, BAZ
}

fun test(e: Entry) {
    <caret>when (e) {
        Entry.FOO -> {}
        else -> {}
    }
}