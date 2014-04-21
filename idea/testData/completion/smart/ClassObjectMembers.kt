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

// EXIST: { lookupString:"K.foo", itemText:"K.foo", tailText:" (sample)", typeText:"K" }
// EXIST: { lookupString:"K.bar", itemText:"K.bar()", tailText:" (sample)", typeText:"K" }
// ABSENT: K.x
// ABSENT: { itemText:"K.kk" }
// EXIST: { lookupString:"K.kk", itemText:"!! K.kk", tailText:" (sample)", typeText:"K?" }
// EXIST: { lookupString:"K.kk", itemText:"?: K.kk", tailText:" (sample)", typeText:"K?" }
// ABSENT: K.privateVal
