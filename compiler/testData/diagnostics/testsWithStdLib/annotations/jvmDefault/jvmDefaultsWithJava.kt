// !API_VERSION: 1.3
// !ENABLE_JVM_DEFAULT
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

interface KotlinInterfaceInderectInheritance : KotlinInterface {
    @JvmDefault
    fun foooo() {
        super.test()

        object  {
            fun run () {
                super@KotlinInterfaceInderectInheritance.test()
            }
        }
    }

    @JvmDefault
    val propertyyy: String
        get() {
            super.test()

            object  {
                fun run () {
                    super@KotlinInterfaceInderectInheritance.test()
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

class KotlinClassInderectInheritance : KotlinClass() {
    fun foo2() {
        super.test()
        super.testOverride()

        object  {
            fun run () {
                super@KotlinClassInderectInheritance.test()
            }
        }
    }

    val property2: String
        get() {
            super.test()
            super.testOverride()

            object  {
                fun run () {
                    super@KotlinClassInderectInheritance.test()
                }
            }
            return ""
        }
}

class KotlinClassInderectInheritance2 : KotlinInterfaceInderectInheritance {
    fun foo() {
        super.test()
        super.testOverride()

        object  {
            fun run () {
                super@KotlinClassInderectInheritance2.test()
            }
        }
    }

    val property: String
        get() {
            super.test()
            super.testOverride()

            object  {
                fun run () {
                    super@KotlinClassInderectInheritance2.test()
                }
            }
            return ""
        }
}

fun test() {
    KotlinClass().foo()
    KotlinClass().property
    KotlinClassInderectInheritance2().foo()
    KotlinClassInderectInheritance2().property

    KotlinClass().test()
    KotlinClass().property
    KotlinClass().testOverride()
    KotlinClassInderectInheritance().testOverride()
}
