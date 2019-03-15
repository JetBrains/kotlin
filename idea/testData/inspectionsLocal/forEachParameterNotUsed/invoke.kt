// PROBLEM: none
// WITH_RUNTIME

class My {
    operator fun invoke() {}
}

fun bar(my: List<My>) {
    my.for<caret>Each { it() }
}