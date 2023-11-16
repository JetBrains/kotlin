// DUMP_IR
// IGNORE_CODEGEN_WITH_IR_FAKE_OVERRIDE_GENERATION
package second

fun box(): String {
    val data = MyClass(object : Base {})
    return data.foo()
}

interface Base {
    fun foo(): String = "OK"
}

class MyClass(val prop: second.Base): Base by prop {
    interface Base
}
