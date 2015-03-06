package sample

class K {
    default object {
        val foo: K = K()
        fun bar(): K = K()
        val x: String = ""
        var kk: K? = null
    }
}

fun foo(){
    val k : K? = <caret>
}

// EXIST: { lookupString:"foo", itemText:"K.foo", tailText:" (sample)", typeText:"K" }
// EXIST: { lookupString:"bar", itemText:"K.bar", tailText:"() (sample)", typeText:"K" }
// ABSENT: { itemText: "K.x" }
// EXIST: { lookupString:"kk", itemText:"K.kk", tailText:" (sample)", typeText:"K?" }
