package sample

class Kool {
    companion object {
        val foo: Kool = Kool()
        fun bar(): Kool = Kool()
    }
}

fun foo(){
    val k : Kool = K<caret>
}

// EXIST: foo
// EXIST: bar
