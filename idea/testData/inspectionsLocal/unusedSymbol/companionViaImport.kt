// PROBLEM: none

import My.Companion.create

class <caret>My {
    companion object {
        fun create() = My()
    }
}

fun test() {
    val my = create()
    my.hashCode()
}