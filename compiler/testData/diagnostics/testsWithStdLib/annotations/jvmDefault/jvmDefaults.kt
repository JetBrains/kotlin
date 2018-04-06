// !API_VERSION: 1.3
// !ENABLE_JVM_DEFAULT
// !JVM_TARGET: 1.8

public interface KInterface {
    @JvmDefault
    fun test(): String {
        return "OK";
    }

    @JvmDefault
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
    @JvmDefault
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

    @JvmDefault
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
        super.<!INTERFACE_CANT_CALL_DEFAULT_METHOD_VIA_SUPER!>test<!>()
        super.<!INTERFACE_CANT_CALL_DEFAULT_METHOD_VIA_SUPER!>property<!>

        super.testNonDefault()
        super.propertyNonDefault

        object  {
            fun run () {
                super@KotlinInterface.<!INTERFACE_CANT_CALL_DEFAULT_METHOD_VIA_SUPER!>test<!>()
                super@KotlinInterface.<!INTERFACE_CANT_CALL_DEFAULT_METHOD_VIA_SUPER!>property<!>

                super@KotlinInterface.testNonDefault()
                super@KotlinInterface.propertyNonDefault
            }
        }
    }

    val propertyyNonDefault: String
        get() {
            super.<!INTERFACE_CANT_CALL_DEFAULT_METHOD_VIA_SUPER!>test<!>()
            super.<!INTERFACE_CANT_CALL_DEFAULT_METHOD_VIA_SUPER!>property<!>

            super.testNonDefault()
            super.propertyNonDefault

            object  {
                fun run () {
                    super@KotlinInterface.<!INTERFACE_CANT_CALL_DEFAULT_METHOD_VIA_SUPER!>test<!>()
                    super@KotlinInterface.<!INTERFACE_CANT_CALL_DEFAULT_METHOD_VIA_SUPER!>property<!>

                    super@KotlinInterface.testNonDefault()
                    super@KotlinInterface.propertyNonDefault
                }
            }
            return ""
        }


}

interface KotlinInterfaceInderectInheritance : KotlinInterface {
    @JvmDefault
    fun foooo() {
        super.test()
        super.property

        super.testNonDefault()
        super.propertyNonDefault

        object  {
            fun run () {
                super@KotlinInterfaceInderectInheritance.test()
                super@KotlinInterfaceInderectInheritance.property

                super@KotlinInterfaceInderectInheritance.testNonDefault()
                super@KotlinInterfaceInderectInheritance.propertyNonDefault
            }
        }
    }

    @JvmDefault
    val propertyyy: String
        get() {
            super.test()
            super.property

            super.testNonDefault()
            super.propertyNonDefault

            object  {
                fun run () {
                    super@KotlinInterfaceInderectInheritance.test()
                    super@KotlinInterfaceInderectInheritance.property

                    super@KotlinInterfaceInderectInheritance.testNonDefault()
                    super@KotlinInterfaceInderectInheritance.propertyNonDefault
                }
            }
            return ""
        }

    fun fooooNonDefault() {
        super.<!INTERFACE_CANT_CALL_DEFAULT_METHOD_VIA_SUPER!>test<!>()
        super.<!INTERFACE_CANT_CALL_DEFAULT_METHOD_VIA_SUPER!>property<!>

        super.testNonDefault()
        super.propertyNonDefault

        object  {
            fun run () {
                super@KotlinInterfaceInderectInheritance.<!INTERFACE_CANT_CALL_DEFAULT_METHOD_VIA_SUPER!>test<!>()
                super@KotlinInterfaceInderectInheritance.<!INTERFACE_CANT_CALL_DEFAULT_METHOD_VIA_SUPER!>property<!>

                super@KotlinInterfaceInderectInheritance.testNonDefault()
                super@KotlinInterfaceInderectInheritance.propertyNonDefault
            }
        }
    }

    val propertyyyNonDefault: String
        get() {
            super.<!INTERFACE_CANT_CALL_DEFAULT_METHOD_VIA_SUPER!>test<!>()
            super.<!INTERFACE_CANT_CALL_DEFAULT_METHOD_VIA_SUPER!>property<!>

            super.testNonDefault()
            super.propertyNonDefault

            object  {
                fun run () {
                    super@KotlinInterfaceInderectInheritance.<!INTERFACE_CANT_CALL_DEFAULT_METHOD_VIA_SUPER!>test<!>()
                    super@KotlinInterfaceInderectInheritance.<!INTERFACE_CANT_CALL_DEFAULT_METHOD_VIA_SUPER!>property<!>

                    super@KotlinInterfaceInderectInheritance.testNonDefault()
                    super@KotlinInterfaceInderectInheritance.propertyNonDefault
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

class KotlinClassInderectInheritance : KotlinClass() {
    fun foo2() {
        super.test()
        super.property

        super.testNonDefault()
        super.propertyNonDefault

        object  {
            fun run () {
                super@KotlinClassInderectInheritance.test()
                super@KotlinClassInderectInheritance.property

                super@KotlinClassInderectInheritance.testNonDefault()
                super@KotlinClassInderectInheritance.propertyNonDefault
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
                    super@KotlinClassInderectInheritance.test()
                    super@KotlinClassInderectInheritance.property

                    super@KotlinClassInderectInheritance.testNonDefault()
                    super@KotlinClassInderectInheritance.propertyNonDefault
                }
            }
            return ""
        }
}

class KotlinClassInderectInheritance2 : KotlinInterfaceInderectInheritance {
    fun foo() {
        super.test()
        super.property

        super.testNonDefault()
        super.propertyNonDefault

        object  {
            fun run () {
                super@KotlinClassInderectInheritance2.test()
                super@KotlinClassInderectInheritance2.property

                super@KotlinClassInderectInheritance2.testNonDefault()
                super@KotlinClassInderectInheritance2.propertyNonDefault
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
                    super@KotlinClassInderectInheritance2.test()
                    super@KotlinClassInderectInheritance2.property

                    super@KotlinClassInderectInheritance2.testNonDefault()
                    super@KotlinClassInderectInheritance2.propertyNonDefault
                }
            }
            return ""
        }
}

fun test() {
    KotlinClass().test()
    KotlinClass().property
    KotlinClass().propertyNonDefault
    KotlinClassInderectInheritance2().test()
    KotlinClassInderectInheritance2().testNonDefault()
    KotlinClassInderectInheritance2().propertyyy
    KotlinClassInderectInheritance2().propertyyyNonDefault

    KotlinClass().test()
    KotlinClass().testNonDefault()
    KotlinClass().property
    KotlinClass().propertyNonDefault
}
