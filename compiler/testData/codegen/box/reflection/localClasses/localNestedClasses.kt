// TARGET_BACKEND: JVM_IR
// WITH_REFLECT

// Android test runner moves the files to different packages
// IGNORE_BACKEND: ANDROID

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
    var res = A().foo()
    if (res != "class A\$foo\$Nested\$Inner") return "Fail 1: $res"
    res = foo3()
    if (res != "class LocalNestedClassesKt\$foo3\$X\$Y\$prop\$1") return "Fail 4: $res"
    res = foo4()
    if (res != "class LocalNestedClassesKt\$foo4\$A\$B\$C\$bar\$D") return "Fail 5: $res"
    res = foo5()
    if (res != "class LocalNestedClassesKt\$foo5\$1\$bar\$1\$foo\$A\$B") return "Fail 6: $res"
    res = foo6()
    if (res != "class LocalNestedClassesKt\$foo6\$1\$bar\$A\$B\$C") return "Fail 7: $res"
    res = foo7()
    if (res != "class LocalNestedClassesKt\$foo7\$x\$1\$y\$1\$z\$1") return "Fail 8: $res"

    return "OK"
}