package sample

class K {
    class object {
        fun bar(p: () -> Unit): K = K()
    }
}

fun foo(){
    val k : K = <caret>
}

// ELEMENT: K.bar
