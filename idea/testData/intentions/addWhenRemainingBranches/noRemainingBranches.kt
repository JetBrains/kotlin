// IS_APPLICABLE: false

enum class Entry {
    FOO, BAR, BAZ
}

fun test(e: Entry) {
    <caret>when (e) {
        Entry.FOO -> {}
        Entry.BAR -> {}
        Entry.BAZ -> {}
    }
}