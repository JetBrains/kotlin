// FIR_IDENTICAL
// !JVM_DEFAULT_MODE: all
// !JVM_TARGET: 1.8

public interface KInterface {
    fun test(): String {
        return "OK";
    }

    val property: String
        get() = "OK"
}

// FILE: 1.kt

interface KotlinInterface : KInterface {
    fun fooo() {
        super.test()
        super.property

        object  {
            fun run () {
                super@KotlinInterface.test()
                super@KotlinInterface.property
            }
        }
    }

    val propertyy: String
        get() {
            super.test()
            super.property

            object  {
                fun run () {
                    super@KotlinInterface.test()
                    super@KotlinInterface.property
                }
            }
            return ""
        }
}

interface KotlinInterfaceIndirectInheritance : KotlinInterface {
    fun foooo() {
        super.test()
        super.property

        object  {
            fun run () {
                super@KotlinInterfaceIndirectInheritance.test()
                super@KotlinInterfaceIndirectInheritance.property
            }
        }
    }

    val propertyyy: String
        get() {
            super.test()
            super.property

            object  {
                fun run () {
                    super@KotlinInterfaceIndirectInheritance.test()
                    super@KotlinInterfaceIndirectInheritance.property
                }
            }
            return ""
        }
}

open class KotlinClass : KInterface {
    fun foo() {
        super.test()
        super.property

        object  {
            fun run () {
                super@KotlinClass.test()
                super@KotlinClass.property
            }
        }
    }

    val xproperty: String
        get() {
            super.test()
            super.property

            object  {
                fun run () {
                    super@KotlinClass.test()
                    super@KotlinClass.property
                }
            }

            return ""
        }
}

class KotlinClassIndirectInheritance : KotlinClass() {
    fun foo2() {
        super.test()
        super.property

        object  {
            fun run () {
                super@KotlinClassIndirectInheritance.test()
                super@KotlinClassIndirectInheritance.property
            }
        }

    }

    val property2: String
        get() {
            super.test()
            super.property

            object  {
                fun run () {
                    super@KotlinClassIndirectInheritance.test()
                    super@KotlinClassIndirectInheritance.property
                }
            }
            return ""
        }
}

class KotlinClassIndirectInheritance2 : KotlinInterfaceIndirectInheritance {
    fun foo() {
        super.test()
        super.property

        object  {
            fun run () {
                super@KotlinClassIndirectInheritance2.test()
                super@KotlinClassIndirectInheritance2.property
            }
        }
    }

    val xproperty: String
        get() {
            super.test()
            super.property

            object  {
                fun run () {
                    super@KotlinClassIndirectInheritance2.test()
                    super@KotlinClassIndirectInheritance2.property
                }
            }
            return ""
        }
}

fun test() {
    KotlinClass().test()
    KotlinClass().property
    KotlinClassIndirectInheritance2().test()
    KotlinClassIndirectInheritance2().propertyyy

    KotlinClass().test()
    KotlinClass().property
}
