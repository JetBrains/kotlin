// FIR_IDENTICAL
fun outer() {
    var x = 0
    fun local() { x++ }
    local()
}