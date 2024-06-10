class MyClass {
    operator fun set(i: Int, v: Int) {}
}

fun main(variable: MyClass) {
    variable[0] <expr>=</expr> 4
}
