// !JVM_DEFAULT_MODE: enable
// !JVM_TARGET: 1.8

public interface KInterface {
    @<!DEPRECATION!>JvmDefault<!>
    fun test(): String {
        return "OK";
    }

    @<!DEPRECATION!>JvmDefault<!>
    val property: String
        get() = "OK"


    fun testNonDefault(): String {
        return "OK";
    }

    val propertyNonDefault: String
        get() = "OK"
}

// FILE: 1.kt

interface KotlinInterface : KInterface {
    @<!DEPRECATION!>JvmDefault<!>
    fun fooo() {
        super.test()
        super.property

        super.testNonDefault()
        super.propertyNonDefault

        object  {
            fun run () {
                super@KotlinInterface.test()
                super@KotlinInterface.property

                super@KotlinInterface.testNonDefault()
                super@KotlinInterface.propertyNonDefault
            }
        }
    }

    @<!DEPRECATION!>JvmDefault<!>
    val propertyy: String
        get() {
            super.test()
            super.property

            super.testNonDefault()
            super.propertyNonDefault

            object  {
                fun run () {
                    super@KotlinInterface.test()
                    super@KotlinInterface.property

                    super@KotlinInterface.testNonDefault()
                    super@KotlinInterface.propertyNonDefault
                }
            }
            return ""
        }

    fun foooNonDefault() {
        super.test()
        super.property

        super.testNonDefault()
        super.propertyNonDefault

        object  {
            fun run () {
                super@KotlinInterface.test()
                super@KotlinInterface.property

                super@KotlinInterface.testNonDefault()
                super@KotlinInterface.propertyNonDefault
            }
        }
    }

    val propertyyNonDefault: String
        get() {
            super.test()
            super.property

            super.testNonDefault()
            super.propertyNonDefault

            object  {
                fun run () {
                    super@KotlinInterface.test()
                    super@KotlinInterface.property

                    super@KotlinInterface.testNonDefault()
                    super@KotlinInterface.propertyNonDefault
                }
            }
            return ""
        }


}

interface KotlinInterfaceIndirectInheritance : KotlinInterface {
    @<!DEPRECATION!>JvmDefault<!>
    fun foooo() {
        super.test()
        super.property

        super.testNonDefault()
        super.propertyNonDefault

        object  {
            fun run () {
                super@KotlinInterfaceIndirectInheritance.test()
                super@KotlinInterfaceIndirectInheritance.property

                super@KotlinInterfaceIndirectInheritance.testNonDefault()
                super@KotlinInterfaceIndirectInheritance.propertyNonDefault
            }
        }
    }

    @<!DEPRECATION!>JvmDefault<!>
    val propertyyy: String
        get() {
            super.test()
            super.property

            super.testNonDefault()
            super.propertyNonDefault

            object  {
                fun run () {
                    super@KotlinInterfaceIndirectInheritance.test()
                    super@KotlinInterfaceIndirectInheritance.property

                    super@KotlinInterfaceIndirectInheritance.testNonDefault()
                    super@KotlinInterfaceIndirectInheritance.propertyNonDefault
                }
            }
            return ""
        }

    fun fooooNonDefault() {
        super.test()
        super.property

        super.testNonDefault()
        super.propertyNonDefault

        object  {
            fun run () {
                super@KotlinInterfaceIndirectInheritance.test()
                super@KotlinInterfaceIndirectInheritance.property

                super@KotlinInterfaceIndirectInheritance.testNonDefault()
                super@KotlinInterfaceIndirectInheritance.propertyNonDefault
            }
        }
    }

    val propertyyyNonDefault: String
        get() {
            super.test()
            super.property

            super.testNonDefault()
            super.propertyNonDefault

            object  {
                fun run () {
                    super@KotlinInterfaceIndirectInheritance.test()
                    super@KotlinInterfaceIndirectInheritance.property

                    super@KotlinInterfaceIndirectInheritance.testNonDefault()
                    super@KotlinInterfaceIndirectInheritance.propertyNonDefault
                }
            }
            return ""
        }
}

open class KotlinClass : KInterface {
    fun foo() {
        super.test()
        super.property

        super.testNonDefault()
        super.propertyNonDefault

        object  {
            fun run () {
                super@KotlinClass.test()
                super@KotlinClass.property

                super@KotlinClass.testNonDefault()
                super@KotlinClass.propertyNonDefault
            }
        }
    }

    val xproperty: String
        get() {
            super.test()
            super.property

            super.testNonDefault()
            super.propertyNonDefault

            object  {
                fun run () {
                    super@KotlinClass.test()
                    super@KotlinClass.property

                    super@KotlinClass.testNonDefault()
                    super@KotlinClass.propertyNonDefault
                }
            }

            return ""
        }
}

class KotlinClassIndirectInheritance : KotlinClass() {
    fun foo2() {
        super.test()
        super.property

        super.testNonDefault()
        super.propertyNonDefault

        object  {
            fun run () {
                super@KotlinClassIndirectInheritance.test()
                super@KotlinClassIndirectInheritance.property

                super@KotlinClassIndirectInheritance.testNonDefault()
                super@KotlinClassIndirectInheritance.propertyNonDefault
            }
        }

    }

    val property2: String
        get() {
            super.test()
            super.property

            super.testNonDefault()
            super.propertyNonDefault

            object  {
                fun run () {
                    super@KotlinClassIndirectInheritance.test()
                    super@KotlinClassIndirectInheritance.property

                    super@KotlinClassIndirectInheritance.testNonDefault()
                    super@KotlinClassIndirectInheritance.propertyNonDefault
                }
            }
            return ""
        }
}

class KotlinClassIndirectInheritance2 : KotlinInterfaceIndirectInheritance {
    fun foo() {
        super.test()
        super.property

        super.testNonDefault()
        super.propertyNonDefault

        object  {
            fun run () {
                super@KotlinClassIndirectInheritance2.test()
                super@KotlinClassIndirectInheritance2.property

                super@KotlinClassIndirectInheritance2.testNonDefault()
                super@KotlinClassIndirectInheritance2.propertyNonDefault
            }
        }
    }

    val xproperty: String
        get() {
            super.test()
            super.property

            super.testNonDefault()
            super.propertyNonDefault

            object  {
                fun run () {
                    super@KotlinClassIndirectInheritance2.test()
                    super@KotlinClassIndirectInheritance2.property

                    super@KotlinClassIndirectInheritance2.testNonDefault()
                    super@KotlinClassIndirectInheritance2.propertyNonDefault
                }
            }
            return ""
        }
}

fun test() {
    KotlinClass().test()
    KotlinClass().property
    KotlinClass().propertyNonDefault
    KotlinClassIndirectInheritance2().test()
    KotlinClassIndirectInheritance2().testNonDefault()
    KotlinClassIndirectInheritance2().propertyyy
    KotlinClassIndirectInheritance2().propertyyyNonDefault

    KotlinClass().test()
    KotlinClass().testNonDefault()
    KotlinClass().property
    KotlinClass().propertyNonDefault
}
