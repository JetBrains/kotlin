// MODULE: original
interface A {
    class B {
        inner class C {
            fun something() {
                println(5)
            }
        }

        private class D {
            fun bar() = 342

            companion object {
                val x = 10
            }
        }
    }

    companion object {
        val x = 5
    }

    fun foo()
}

// MODULE: copy
interface A {
    class B {
        inner class C {
            fun something() {
                println(5)
            }
        }

        private class D {
            private fun bar() = 342

            companion object {
                val x = 10
            }
        }
    }

    companion object {
        val x = 5
    }

    fun foo()
}