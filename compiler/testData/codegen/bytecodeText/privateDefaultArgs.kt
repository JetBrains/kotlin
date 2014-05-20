// Check that this code doesn't contains INVOKEVIRTUAL instruction
class B {
    private fun foo(i: Int = 1) {
    }

    fun f() {
       foo(2)
    }
}

fun box(): String {
    return "OK"
}

// 0 INVOKEVIRTUAL
// 2 INVOKESPECIAL B\.foo
