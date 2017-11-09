// "Create label foo@" "true"

fun test() {
    while (true) {
        break@<caret>foo
    }
}