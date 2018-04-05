// FILE: JavaInterface.java

public interface JavaInterface {
    static String testStatic() {
        return "OK";
    }

    default String test() {
        return "OK";
    }

    default String testOverride() {
        return "OK";
    }
}

// FILE: 1.kt
import JavaInterface.testStatic

interface KotlinInterface : JavaInterface {
    fun fooo() {
        <!INTERFACE_STATIC_METHOD_CALL_FROM_JAVA6_TARGET_ERROR!>testStatic<!>()
        super.<!INTERFACE_CANT_CALL_DEFAULT_METHOD_VIA_SUPER!>test<!>()

        object  {
            fun run () {
                super@KotlinInterface.<!INTERFACE_CANT_CALL_DEFAULT_METHOD_VIA_SUPER!>test<!>()
            }
        }
    }

    val propertyy: String
        get() {
            super.<!INTERFACE_CANT_CALL_DEFAULT_METHOD_VIA_SUPER!>test<!>()

            object  {
                fun run () {
                    super@KotlinInterface.<!INTERFACE_CANT_CALL_DEFAULT_METHOD_VIA_SUPER!>test<!>()
                }
            }
            return ""
        }

    override fun testOverride(): String {
        return "OK";
    }
}

interface KotlinInterfaceInderectInheritance : KotlinInterface {
    fun foooo() {
        <!INTERFACE_STATIC_METHOD_CALL_FROM_JAVA6_TARGET_ERROR!>testStatic<!>()
        super.<!INTERFACE_CANT_CALL_DEFAULT_METHOD_VIA_SUPER!>test<!>()

        object  {
            fun run () {
                super@KotlinInterfaceInderectInheritance.<!INTERFACE_CANT_CALL_DEFAULT_METHOD_VIA_SUPER!>test<!>()
            }
        }
    }

    val propertyyy: String
        get() {
            super.<!INTERFACE_CANT_CALL_DEFAULT_METHOD_VIA_SUPER!>test<!>()

            object  {
                fun run () {
                    super@KotlinInterfaceInderectInheritance.<!INTERFACE_CANT_CALL_DEFAULT_METHOD_VIA_SUPER!>test<!>()
                }
            }
            return ""
        }
}

open class KotlinClass : JavaInterface {
    fun foo() {
        <!INTERFACE_STATIC_METHOD_CALL_FROM_JAVA6_TARGET_ERROR!>testStatic<!>()
        super.<!DEFAULT_METHOD_CALL_FROM_JAVA6_TARGET_ERROR!>test<!>()
        super.<!DEFAULT_METHOD_CALL_FROM_JAVA6_TARGET_ERROR!>testOverride<!>()

        object  {
            fun run () {
                super@KotlinClass.<!DEFAULT_METHOD_CALL_FROM_JAVA6_TARGET_ERROR!>test<!>()
            }
        }
    }

    val property: String
        get() {
            super.<!DEFAULT_METHOD_CALL_FROM_JAVA6_TARGET_ERROR!>test<!>()
            super.<!DEFAULT_METHOD_CALL_FROM_JAVA6_TARGET_ERROR!>testOverride<!>()

            object  {
                fun run () {
                    super@KotlinClass.<!DEFAULT_METHOD_CALL_FROM_JAVA6_TARGET_ERROR!>test<!>()
                }
            }
            return ""
        }
}

class KotlinClassInderectInheritance : KotlinClass() {
    fun foo2(){
        <!INTERFACE_STATIC_METHOD_CALL_FROM_JAVA6_TARGET_ERROR!>testStatic<!>()
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
        <!INTERFACE_STATIC_METHOD_CALL_FROM_JAVA6_TARGET_ERROR!>testStatic<!>()
        super.<!DEFAULT_METHOD_CALL_FROM_JAVA6_TARGET_ERROR!>test<!>()
        super.testOverride()

        object  {
            fun run () {
                super@KotlinClassInderectInheritance2.<!DEFAULT_METHOD_CALL_FROM_JAVA6_TARGET_ERROR!>test<!>()
            }
        }
    }

    val property: String
        get() {
            super.<!DEFAULT_METHOD_CALL_FROM_JAVA6_TARGET_ERROR!>test<!>()
            super.testOverride()

            object  {
                fun run () {
                    super@KotlinClassInderectInheritance2.<!DEFAULT_METHOD_CALL_FROM_JAVA6_TARGET_ERROR!>test<!>()
                }
            }
            return ""
        }
}

fun test() {
    JavaInterface.<!INTERFACE_STATIC_METHOD_CALL_FROM_JAVA6_TARGET_ERROR!>testStatic<!>()
    KotlinClass().foo()
    KotlinClass().property
    KotlinClassInderectInheritance2().foo()
    KotlinClassInderectInheritance2().property

    KotlinClass().test()
    KotlinClass().property
    KotlinClass().testOverride()
    KotlinClassInderectInheritance().testOverride()
}
