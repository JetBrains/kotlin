fun box() {
    class Foo {
        fun foo() {
            <expr>Bar()</expr>.unknown()
        }

        private inner class Bar: Unknown<Unit, Unit, Int>() {
            override fun unknown() {}
        }
    }
}