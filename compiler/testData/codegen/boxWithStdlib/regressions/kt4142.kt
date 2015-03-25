open class B {
    val name: String
        get() = "OK"
}

trait A {
    val name: String
}

class C : B(), A {

}

fun box(): String {
    return C().name
}
