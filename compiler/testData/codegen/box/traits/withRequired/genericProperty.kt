abstract class Base<T> {
    abstract var s: T
}

trait Trait<T> : Base<T> {
    var value : T
        get() = s
        set(value) { s = value }
}

class Derived : Trait<String>, Base<String>() {
    override var s = "Fail"
}

fun box(): String {
    val d = Derived()
    d.value = "OK"
    return d.value
}
