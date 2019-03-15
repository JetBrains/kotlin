// !JVM_DEFAULT_MODE: enable
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

interface KotlinInterfaceIndirectInheritance : KotlinInterface {
    @JvmDefault
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

    @JvmDefault
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
        super.<!INTERFACE_CANT_CALL_DEFAULT_METHOD_VIA_SUPER!>test<!>()
        super.<!INTERFACE_CANT_CALL_DEFAULT_METHOD_VIA_SUPER!>property<!>

        super.testNonDefault()
        super.propertyNonDefault

        object  {
            fun run () {
                super@KotlinInterfaceIndirectInheritance.<!INTERFACE_CANT_CALL_DEFAULT_METHOD_VIA_SUPER!>test<!>()
                super@KotlinInterfaceIndirectInheritance.<!INTERFACE_CANT_CALL_DEFAULT_METHOD_VIA_SUPER!>property<!>

                super@KotlinInterfaceIndirectInheritance.testNonDefault()
                super@KotlinInterfaceIndirectInheritance.propertyNonDefault
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
                    super@KotlinInterfaceIndirectInheritance.<!INTERFACE_CANT_CALL_DEFAULT_METHOD_VIA_SUPER!>test<!>()
                    super@KotlinInterfaceIndirectInheritance.<!INTERFACE_CANT_CALL_DEFAULT_METHOD_VIA_SUPER!>property<!>

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
