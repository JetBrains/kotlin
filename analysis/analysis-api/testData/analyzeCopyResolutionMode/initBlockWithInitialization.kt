// MODULE: original
class A {
    var x: Int = 5
    init {}
}

// MODULE: copy
class A {
    var x: Int = 5
    init {
        x = 6
    }
}