class MyClass {
    operator fun set(i: Int, v: Int) {}
}

fun main(variable: MyClass) {
    <expr>variable[0] = 4</expr>
}