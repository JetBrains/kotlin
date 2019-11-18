// IGNORE_BACKEND_FIR: JVM_IR
var result = "Fail"

interface A {
    var foo: String
        get() = result
        set(value) { result = value }
}

interface B : A

class C : B {
    override var foo: String
        get() = super.foo
        set(value) { super.foo = value }
}

fun box(): String {
    C().foo = "OK"
    return C().foo
}
