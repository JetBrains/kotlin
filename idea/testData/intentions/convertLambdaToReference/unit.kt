// IS_APPLICABLE: false

fun Int.exec(f: (Int) -> Unit) = f(this)

fun bar(x: Int) = x

fun foo() {
    2.exec {<caret> bar(it) }
}
