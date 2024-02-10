// TARGET_BACKEND: JVM_IR
// WITH_REFLECT
// JVM_ABI_K1_K2_DIFF: K2 names companion objects in metadata correctly

import kotlin.reflect.KType
import kotlin.reflect.full.memberProperties

val KType.str get() = classifier.toString()

class A {
    fun foo(): String {
        class Nested {
            inner class Inner {
                val prop = this
            }
        }
        return Nested().Inner()::class.memberProperties.iterator().next().returnType.str
    }
}

fun foo1(): String {
    class X {
        inner class Y {
            companion object Z

            val prop = Z
        }
    }
    return X.Y::class.memberProperties.iterator().next().returnType.str
}

fun foo2(): String {
    class X {
        inner class Y {
            companion object

            val prop = Companion
        }
    }
    return X.Y::class.memberProperties.iterator().next().returnType.str
}

fun foo3(): String {
    class X {
        inner class Y {
            val prop = object {}
        }
    }
    return X.Y::class.memberProperties.iterator().next().returnType.str
}

fun foo4(): String {
    var res = ""

    class A {
        inner class B {
            inner class C {
                fun bar() {
                    class D {
                        val prop = this
                    }
                    res = D::class.memberProperties.iterator().next().returnType.str
                }

                init {
                    bar()
                }
            }
        }
    }
    A().B().C()
    return res
}

fun foo5(): String {
    var res = ""
    object {
        fun bar() {
            return object {
                fun foo() {
                    class A {
                        inner class B {
                            val prop = this
                            init {
                                res = prop::class.memberProperties.iterator().next().returnType.str
                            }
                        }
                    }
                    A().B()
                }
            }.foo()
        }
    }.bar()
    return res
}

fun foo6(): String {
    var res = ""
    object {
        fun bar() {
            class A {
                inner class B {
                    inner class C {
                        val prop = this

                        init {
                            res = prop::class.memberProperties.iterator().next().returnType.str
                        }
                    }
                }
            }
            A().B().C()
        }
    }.bar()
    return res
}

fun foo7(): String {
    var res = ""
    val x = object {
        val y = object {
            val z = object {
                val y = this
                init {
                    res = this::class.memberProperties.iterator().next().returnType.str
                }
            }
        }
    }
    return res
}

fun box(): String {
    if (A().foo() != "class A\$foo\$Nested\$Inner") return "Fail 1"
    if (foo1() != "class LocalNestedClassesKt\$foo1\$X\$Y\$Z") return "Fail 2"
    if (foo2() != "class LocalNestedClassesKt\$foo2\$X\$Y\$Companion") return "Fail 3"
    if (foo3() != "class LocalNestedClassesKt\$foo3\$X\$Y\$prop\$1") return "Fail 4"
    if (foo4() != "class LocalNestedClassesKt\$foo4\$A\$B\$C\$bar\$D") return "Fail 5"
    if (foo5() != "class LocalNestedClassesKt\$foo5\$1\$bar\$1\$foo\$A\$B") return "Fail 6"
    if (foo6() != "class LocalNestedClassesKt\$foo6\$1\$bar\$A\$B\$C") return "Fail 7"
    if (foo7() != "class LocalNestedClassesKt\$foo7\$x\$1\$y\$1\$z\$1") return "Fail 8"

    return "OK"
}