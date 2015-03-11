class K {
    default object {
        fun bar(): K? = K()
    }
}

fun foo(){
    val k : K = <caret>
}

// ELEMENT_TEXT: "!! K.bar"
// TAIL_TEXT: "()"
