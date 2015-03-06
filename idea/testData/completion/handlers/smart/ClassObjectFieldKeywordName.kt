package `package`

class `class` {
    default object {
        val `val` = `class`()
    }
}

fun foo(){
    val v: `class`= <caret>
}

// ELEMENT: val
