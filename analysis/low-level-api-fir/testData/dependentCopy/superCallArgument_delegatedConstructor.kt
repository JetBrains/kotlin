package test

val property = 10

class MyClass(i: Int) {
    constructor(): this(prop<caret>erty)
}