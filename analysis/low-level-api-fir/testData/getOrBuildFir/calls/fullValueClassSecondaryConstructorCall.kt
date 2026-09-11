// LANGUAGE: +FullValueClasses
value class Point(val x: Int, val y: Int) {
    constructor(value: Int) : this(value, value)
}

fun test() {
    <expr>Point(1)</expr>
}
