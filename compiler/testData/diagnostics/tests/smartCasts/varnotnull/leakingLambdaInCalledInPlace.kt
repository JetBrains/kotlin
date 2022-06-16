// FIR_IDENTICAL
fun main() {
    var p: String?
    var block: () -> Int = { 1 }
    p = "2"
    run {
        block = { <!SMARTCAST_IMPOSSIBLE!>p<!>.length }
    }
    p = null
    block()
}
