package test

class MyClass<T>(t: T)

class Cell<T>(value: T)

typealias MyTypeAliasFirst<T1> = MyClass<Cell<T1>>

typealias MyTypeAliasSecond<T2> = MyTypeAliasFirst<Cell<T2>>

fun usage() {
    <caret>MyTypeAliasSecond(Cell(Cell("")))
}
