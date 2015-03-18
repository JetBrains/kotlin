package sample

class K {
    companion object {
        fun bar(p: () -> Unit): K = K()
    }
}

fun foo(){
    val k : K = <caret>
}

// ELEMENT: bar
