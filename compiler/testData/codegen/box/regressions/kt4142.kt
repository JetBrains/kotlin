open class B {
    val name: String
        get() = "OK"
}

interface A {
    val name: String
}

class C : B(), A {

}

fun box(): String {
    return C().name
}
