package sample

class K {
    class object {
        val foo: K = K()
        fun bar(): K = K()
        val x: String = ""
        var kk: K? = null
        private val privateVal: K = K()
    }
}

fun foo(){
    val k : K = <caret>
}

// EXIST: { lookupString:"foo", itemText:"K.foo", tailText:" (sample)", typeText:"K" }
// EXIST: { lookupString:"bar", itemText:"K.bar()", tailText:" (sample)", typeText:"K" }
// ABSENT: { itemText: "K.x" }
// ABSENT: { itemText:"K.kk" }
// EXIST: { lookupString:"kk", itemText:"!! K.kk", tailText:" (sample)", typeText:"K?" }
// EXIST: { lookupString:"kk", itemText:"?: K.kk", tailText:" (sample)", typeText:"K?" }
// ABSENT: { itemText: "K.privateVal" }
