// FIR_IDENTICAL
// !JVM_DEFAULT_MODE: enable
// !JVM_TARGET: 1.8
// FILE: JavaInterface.java

public interface JavaInterface {
    default String test() {
        return "OK";
    }

    default String testOverride() {
        return "OK";
    }
}

// FILE: 1.kt

interface KotlinInterface : JavaInterface {
    @JvmDefault
    fun fooo() {
        super.test()

        object  {
            fun run () {
                super@KotlinInterface.test()
            }
        }
    }

    @JvmDefault
    val propertyy: String
        get() {
            super.test()

            object  {
                fun run () {
                    super@KotlinInterface.test()
                }
            }
            return ""
        }

    @JvmDefault
    override fun testOverride(): String {
        return "OK";
    }
}

interface KotlinInterfaceIndirectInheritance : KotlinInterface {
    @JvmDefault
    fun foooo() {
        super.test()

        object  {
            fun run () {
                super@KotlinInterfaceIndirectInheritance.test()
            }
        }
    }

    @JvmDefault
    val propertyyy: String
        get() {
            super.test()

            object  {
                fun run () {
                    super@KotlinInterfaceIndirectInheritance.test()
                }
            }
            return ""
        }
}

open class KotlinClass : JavaInterface {
    fun foo() {
        super.test()
        super.testOverride()

        object  {
            fun run () {
                super@KotlinClass.test()
            }
        }
    }

    val property: String
        get() {
            super.test()
            super.testOverride()

            object  {
                fun run () {
                    super@KotlinClass.test()
                }
            }
            return ""
        }
}

class KotlinClassIndirectInheritance : KotlinClass() {
    fun foo2() {
        super.test()
        super.testOverride()

        object  {
            fun run () {
                super@KotlinClassIndirectInheritance.test()
            }
        }
    }

    val property2: String
        get() {
            super.test()
            super.testOverride()

            object  {
                fun run () {
                    super@KotlinClassIndirectInheritance.test()
                }
            }
            return ""
        }
}

class KotlinClassIndirectInheritance2 : KotlinInterfaceIndirectInheritance {
    fun foo() {
        super.test()
        super.testOverride()

        object  {
            fun run () {
                super@KotlinClassIndirectInheritance2.test()
            }
        }
    }

    val property: String
        get() {
            super.test()
            super.testOverride()

            object  {
                fun run () {
                    super@KotlinClassIndirectInheritance2.test()
                }
            }
            return ""
        }
}

fun test() {
    KotlinClass().foo()
    KotlinClass().property
    KotlinClassIndirectInheritance2().foo()
    KotlinClassIndirectInheritance2().property

    KotlinClass().test()
    KotlinClass().property
    KotlinClass().testOverride()
    KotlinClassIndirectInheritance().testOverride()
}
