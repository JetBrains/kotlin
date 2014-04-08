open class Base {
    var s = "Fail"
}

trait Trait : Base {
    var value : String
        get() = s
        set(value) { s = value }
}

class Derived : Trait, Base()

fun box(): String {
    val d = Derived()
    d.value = "OK"
    return d.value
}
