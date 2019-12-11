// See KT-20959

enum class Foo {;
    companion object  {
        val x = foo() // there should be no UNINITIALIZED_ENUM_COMPANION

        private fun foo() = "OK"
    }
}
