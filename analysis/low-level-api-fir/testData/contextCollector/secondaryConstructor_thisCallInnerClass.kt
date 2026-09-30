package test

class Outer {
    inner class Child(primaryConstructorParameter: Int) {
        constructor() : <expr>this(10)</expr>
    }
}
