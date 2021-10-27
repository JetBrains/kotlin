// !DIAGNOSTICS: -UNUSED_VARIABLE
package o

class TestFunctionLiteral {
    val sum: (Int) -> Int = { x: Int ->
        sum(x - 1) + x
    }
    val foo: () -> Unit = l@ ({ foo() })
}

open class A(val a: A)

class TestObjectLiteral {
    val obj: A = object: A(obj) {
        init {
            val x = obj
        }
        fun foo() {
            val y = obj
        }
    }
    val obj1: A = l@ ( object: A(obj1) {
        init {
            val x = obj1
        }
        fun foo() = obj1
    })
}

class TestOther {
    val x: Int = x + 1
}
