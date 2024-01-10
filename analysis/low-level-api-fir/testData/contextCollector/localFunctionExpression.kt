fun test() {
    fun local(n: Int) {
        <expr>call(n)</expr>
    }
    local(5)
}

fun call(obj: Any?) {}