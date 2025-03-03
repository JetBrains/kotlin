package test.class_object

class ClassObject {
    fun f() {
    }

    val c = 1

    public companion object {
        val j = 0
        fun z() = 0

        class A {
            class B {
                val i: Int = 0
                fun f() = 0
            }
        }
    }


    class B {
        companion object {
            class C {
                companion object {
                    class D {
                        companion object {
                            val i = 3
                            fun f() {
                            }

                            enum class En

                            annotation class Anno
                        }
                    }
                }
            }
        }
    }
}