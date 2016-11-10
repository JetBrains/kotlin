package a

class B {
    /**
     * [a.B.<caret>]
     */
    fun member() {

    }
}

fun B.ext() {

}

val B.extVal: String
    get() = ""

// EXIST: ext
// EXIST: extVal