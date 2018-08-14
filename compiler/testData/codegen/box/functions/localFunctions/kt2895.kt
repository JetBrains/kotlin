fun outer() {
    fun inner(i: Int) {
        if (i > 0){
            {
                it: Int -> inner(0) // <- invocation of literal itself is generated instead
            }.invoke(1)
        }
    }
    inner(1)
}

fun box(): String {
    outer()
    return "OK"
}
