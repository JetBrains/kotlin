// !LANGUAGE: -DefaultMethodsCallFromJava6TargetError
// !JVM_TARGET: 1.6
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
        <!INTERFACE_STATIC_METHOD_CALL_FROM_JAVA6_TARGET!>testStatic<!>()
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

interface KotlinInterfaceIndirectInheritance : KotlinInterface {
    fun foooo() {
        <!INTERFACE_STATIC_METHOD_CALL_FROM_JAVA6_TARGET!>testStatic<!>()
        super.<!INTERFACE_CANT_CALL_DEFAULT_METHOD_VIA_SUPER!>test<!>()

        object  {
            fun run () {
                super@KotlinInterfaceIndirectInheritance.<!INTERFACE_CANT_CALL_DEFAULT_METHOD_VIA_SUPER!>test<!>()
            }
        }
    }

    val propertyyy: String
        get() {
            super.<!INTERFACE_CANT_CALL_DEFAULT_METHOD_VIA_SUPER!>test<!>()

            object  {
                fun run () {
                    super@KotlinInterfaceIndirectInheritance.<!INTERFACE_CANT_CALL_DEFAULT_METHOD_VIA_SUPER!>test<!>()
                }
            }
            return ""
        }
}

open class KotlinClass : JavaInterface {
    fun foo(){
        <!INTERFACE_STATIC_METHOD_CALL_FROM_JAVA6_TARGET!>testStatic<!>()
        super.<!DEFAULT_METHOD_CALL_FROM_JAVA6_TARGET!>test<!>()
        super.<!DEFAULT_METHOD_CALL_FROM_JAVA6_TARGET!>testOverride<!>()

        object  {
            fun run () {
                super@KotlinClass.<!DEFAULT_METHOD_CALL_FROM_JAVA6_TARGET!>test<!>()
            }
        }
    }

    val property: String
        get() {
            super.<!DEFAULT_METHOD_CALL_FROM_JAVA6_TARGET!>test<!>()
            super.<!DEFAULT_METHOD_CALL_FROM_JAVA6_TARGET!>testOverride<!>()

            object  {
                fun run () {
                    super@KotlinClass.<!DEFAULT_METHOD_CALL_FROM_JAVA6_TARGET!>test<!>()
                }
            }
            return ""
        }
}

class KotlinClassIndirectInheritance : KotlinClass() {
    fun foo2(){
        <!INTERFACE_STATIC_METHOD_CALL_FROM_JAVA6_TARGET!>testStatic<!>()
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
    fun foo(){
        <!INTERFACE_STATIC_METHOD_CALL_FROM_JAVA6_TARGET!>testStatic<!>()
        super.<!DEFAULT_METHOD_CALL_FROM_JAVA6_TARGET!>test<!>()
        super.testOverride()

        object  {
            fun run () {
                super@KotlinClassIndirectInheritance2.<!DEFAULT_METHOD_CALL_FROM_JAVA6_TARGET!>test<!>()
            }
        }
    }

    val property: String
        get() {
            super.<!DEFAULT_METHOD_CALL_FROM_JAVA6_TARGET!>test<!>()
            super.testOverride()

            object  {
                fun run () {
                    super@KotlinClassIndirectInheritance2.<!DEFAULT_METHOD_CALL_FROM_JAVA6_TARGET!>test<!>()
                }
            }
            return ""
        }
}

fun test() {
    JavaInterface.<!INTERFACE_STATIC_METHOD_CALL_FROM_JAVA6_TARGET!>testStatic<!>()
    KotlinClass().foo()
    KotlinClass().property
    KotlinClassIndirectInheritance2().foo()
    KotlinClassIndirectInheritance2().property

    KotlinClass().test()
    KotlinClass().property
    KotlinClass().testOverride()
    KotlinClassIndirectInheritance().testOverride()
}
