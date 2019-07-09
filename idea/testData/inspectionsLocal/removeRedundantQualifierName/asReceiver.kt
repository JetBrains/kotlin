// WITH_RUNTIME

import B.G

fun test() {
    <caret>B.G.let { it }
}

class B {
    object G
}

class C {
    object G
}
