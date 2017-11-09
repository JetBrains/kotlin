val <caret>property: Int
    get {
        println("access!")
        return 1
    }

fun foo() {
    println(property)
}