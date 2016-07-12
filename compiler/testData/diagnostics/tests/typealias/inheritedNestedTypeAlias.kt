interface ICell<T> {
    val x: T
}

class Cell<T>(override val x: T): ICell<T>

open class Base<T> {
    typealias CT = Cell<T>
    inner class InnerCell(override val x: T): ICell<T>
}

class Derived : Base<Int>() {
    // TODO KT-11123

    val x1: <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>InnerCell<!> = InnerCell(42)
    val x2: Base<Int>.InnerCell = InnerCell(42)

    val test1: <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>CT<!> = Cell(42)
    val test2: Base<Int>.CT = Cell(42)
}
