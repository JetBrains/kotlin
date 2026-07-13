class MyClass {
    override fun equals(other: Any?): Int {
        return 5
    }
}

fun main() {
    <expr>MyClass() == MyClass()</expr>
}
