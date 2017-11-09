// "Create label foo@" "true"

inline fun Int.bar(f: (Int) -> Unit) { }

fun test() {
    1.bar { if (it == 2) return@<caret>foo }
}