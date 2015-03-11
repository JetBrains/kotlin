package test.class_object

class ClassObject {
    fun f() {
    }

    val c = 1

    public default object {
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
        default object {
            class C {
                default object {
                    class D {
                        default object {
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