package sample

class K {
    default object {
        fun bar(p: Int): K = K()
    }
}

fun foo(){
    val k : K = <caret>
}

// ELEMENT: bar
