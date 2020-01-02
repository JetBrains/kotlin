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
                super@KotlinInterface.<!UNRESOLVED_REFERENCE!>test<!>()
                super@KotlinInterface.<!UNRESOLVED_REFERENCE!>property<!>

                super@KotlinInterface.<!UNRESOLVED_REFERENCE!>testNonDefault<!>()
                super@KotlinInterface.<!UNRESOLVED_REFERENCE!>propertyNonDefault<!>
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
                    super@KotlinInterface.<!UNRESOLVED_REFERENCE!>test<!>()
                    super@KotlinInterface.<!UNRESOLVED_REFERENCE!>property<!>

                    super@KotlinInterface.<!UNRESOLVED_REFERENCE!>testNonDefault<!>()
                    super@KotlinInterface.<!UNRESOLVED_REFERENCE!>propertyNonDefault<!>
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
                super@KotlinInterface.<!UNRESOLVED_REFERENCE!>test<!>()
                super@KotlinInterface.<!UNRESOLVED_REFERENCE!>property<!>

                super@KotlinInterface.<!UNRESOLVED_REFERENCE!>testNonDefault<!>()
                super@KotlinInterface.<!UNRESOLVED_REFERENCE!>propertyNonDefault<!>
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
                    super@KotlinInterface.<!UNRESOLVED_REFERENCE!>test<!>()
                    super@KotlinInterface.<!UNRESOLVED_REFERENCE!>property<!>

                    super@KotlinInterface.<!UNRESOLVED_REFERENCE!>testNonDefault<!>()
                    super@KotlinInterface.<!UNRESOLVED_REFERENCE!>propertyNonDefault<!>
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
                super@KotlinInterfaceIndirectInheritance.<!UNRESOLVED_REFERENCE!>test<!>()
                super@KotlinInterfaceIndirectInheritance.<!UNRESOLVED_REFERENCE!>property<!>

                super@KotlinInterfaceIndirectInheritance.<!UNRESOLVED_REFERENCE!>testNonDefault<!>()
                super@KotlinInterfaceIndirectInheritance.<!UNRESOLVED_REFERENCE!>propertyNonDefault<!>
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
                    super@KotlinInterfaceIndirectInheritance.<!UNRESOLVED_REFERENCE!>test<!>()
                    super@KotlinInterfaceIndirectInheritance.<!UNRESOLVED_REFERENCE!>property<!>

                    super@KotlinInterfaceIndirectInheritance.<!UNRESOLVED_REFERENCE!>testNonDefault<!>()
                    super@KotlinInterfaceIndirectInheritance.<!UNRESOLVED_REFERENCE!>propertyNonDefault<!>
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
                super@KotlinInterfaceIndirectInheritance.<!UNRESOLVED_REFERENCE!>test<!>()
                super@KotlinInterfaceIndirectInheritance.<!UNRESOLVED_REFERENCE!>property<!>

                super@KotlinInterfaceIndirectInheritance.<!UNRESOLVED_REFERENCE!>testNonDefault<!>()
                super@KotlinInterfaceIndirectInheritance.<!UNRESOLVED_REFERENCE!>propertyNonDefault<!>
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
                    super@KotlinInterfaceIndirectInheritance.<!UNRESOLVED_REFERENCE!>test<!>()
                    super@KotlinInterfaceIndirectInheritance.<!UNRESOLVED_REFERENCE!>property<!>

                    super@KotlinInterfaceIndirectInheritance.<!UNRESOLVED_REFERENCE!>testNonDefault<!>()
                    super@KotlinInterfaceIndirectInheritance.<!UNRESOLVED_REFERENCE!>propertyNonDefault<!>
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
                super@KotlinClass.<!UNRESOLVED_REFERENCE!>test<!>()
                super@KotlinClass.<!UNRESOLVED_REFERENCE!>property<!>

                super@KotlinClass.<!UNRESOLVED_REFERENCE!>testNonDefault<!>()
                super@KotlinClass.<!UNRESOLVED_REFERENCE!>propertyNonDefault<!>
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
                    super@KotlinClass.<!UNRESOLVED_REFERENCE!>test<!>()
                    super@KotlinClass.<!UNRESOLVED_REFERENCE!>property<!>

                    super@KotlinClass.<!UNRESOLVED_REFERENCE!>testNonDefault<!>()
                    super@KotlinClass.<!UNRESOLVED_REFERENCE!>propertyNonDefault<!>
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
                super@KotlinClassIndirectInheritance.<!UNRESOLVED_REFERENCE!>test<!>()
                super@KotlinClassIndirectInheritance.<!UNRESOLVED_REFERENCE!>property<!>

                super@KotlinClassIndirectInheritance.<!UNRESOLVED_REFERENCE!>testNonDefault<!>()
                super@KotlinClassIndirectInheritance.<!UNRESOLVED_REFERENCE!>propertyNonDefault<!>
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
                    super@KotlinClassIndirectInheritance.<!UNRESOLVED_REFERENCE!>test<!>()
                    super@KotlinClassIndirectInheritance.<!UNRESOLVED_REFERENCE!>property<!>

                    super@KotlinClassIndirectInheritance.<!UNRESOLVED_REFERENCE!>testNonDefault<!>()
                    super@KotlinClassIndirectInheritance.<!UNRESOLVED_REFERENCE!>propertyNonDefault<!>
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
                super@KotlinClassIndirectInheritance2.<!UNRESOLVED_REFERENCE!>test<!>()
                super@KotlinClassIndirectInheritance2.<!UNRESOLVED_REFERENCE!>property<!>

                super@KotlinClassIndirectInheritance2.<!UNRESOLVED_REFERENCE!>testNonDefault<!>()
                super@KotlinClassIndirectInheritance2.<!UNRESOLVED_REFERENCE!>propertyNonDefault<!>
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
                    super@KotlinClassIndirectInheritance2.<!UNRESOLVED_REFERENCE!>test<!>()
                    super@KotlinClassIndirectInheritance2.<!UNRESOLVED_REFERENCE!>property<!>

                    super@KotlinClassIndirectInheritance2.<!UNRESOLVED_REFERENCE!>testNonDefault<!>()
                    super@KotlinClassIndirectInheritance2.<!UNRESOLVED_REFERENCE!>propertyNonDefault<!>
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
