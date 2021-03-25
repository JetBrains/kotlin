// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_PARAMETER
fun main(d : D) {
    d.from("")
    d.from(1)
}

class D {
    fun from(vararg a : Any){}
    fun from(vararg a : String){}
}