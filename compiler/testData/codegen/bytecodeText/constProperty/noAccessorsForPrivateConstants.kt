// FILE: Foo.kt

private const val OUTER_PRIVATE = 20

class Foo {
    companion object {
        private const val LOCAL_PRIVATE = 20
    }

    fun foo() {
        LOCAL_PRIVATE
        OUTER_PRIVATE
    }
}

// 0 INVOKESTATIC