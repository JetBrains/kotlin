package sample

class K {
    class object {
        fun bar(p: (Int, String) -> Unit): K = K()
    }
}

fun foo(){
    val k : K = <caret>
}

// ELEMENT: K.bar
