open class A() {
    open val method : () -> Unit? = {println("hello")}
}

fun some() : A {
    return object : A() {<caret>}
}
