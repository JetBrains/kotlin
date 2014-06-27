import test.genericConstructor

class Subclass : genericConstructor<Int>(42) {
}

fun box(): String {
    Subclass()
    return "OK"
}
