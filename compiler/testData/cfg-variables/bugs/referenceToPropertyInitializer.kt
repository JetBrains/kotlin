class TestFunctionLiteral {
    val sum: (Int)->Int = { x: Int ->
        sum(x - 1) + x
    }
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
}

class TestOther {
    val x: Int = x + 1
}