package test

class NamedCompanionObject {
    fun f() {
    }

    val c = 1

    public companion object Named {
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
        companion object NamedInB {
            class C {
                companion object NamedInC {
                    class D {
                        companion object Companion {
                            val i = 3
                            fun f() {
                            }

                            enum class En {
                                A;

                                companion object NamedInEn
                            }

                            annotation class Anno
                        }
                    }
                }
            }
        }
    }
}