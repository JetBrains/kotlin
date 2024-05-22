class Test(
    val p: Int,
) {
    operator fun plus(other: Test) = Test(this.p + other.p)
}

fun test() {
    var x = Test(40)
    <expr>x += Test(2)</expr>
}