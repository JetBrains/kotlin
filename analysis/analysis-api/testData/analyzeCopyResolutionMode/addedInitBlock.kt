// MODULE: original
class A(val x: Int) {

}

// MODULE: copy
class A(val x: Int) {
    init {
        // do something
        println("hello")
    }
}