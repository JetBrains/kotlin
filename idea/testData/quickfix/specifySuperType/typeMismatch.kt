// "Specify supertype" "true"
// DISABLE-ERRORS
interface Z {
    fun foo(): CharSequence = ""
}

open class Y {
    override fun foo(): String = ""
}

class Test : Z, Y() {
    override fun foo(): String {
        return <caret>super.foo()
    }
}