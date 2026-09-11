// LANGUAGE: +FullValueClasses
value class Point(val x: Int, val y: Int) {
    constructor(value: Int) : <expr>this(value, value)</expr>
}
