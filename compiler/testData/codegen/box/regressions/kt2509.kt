// IGNORE_BACKEND_FIR: JVM_IR
fun box(): String {
        A()
        return "OK"
}

class A: B() {
        override var foo = arrayOf<Int?>(12, 13)
}

abstract class B {
        abstract var foo: Array<Int?>
}
