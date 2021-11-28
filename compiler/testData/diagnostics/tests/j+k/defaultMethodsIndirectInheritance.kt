// !JVM_TARGET: 1.6
// FILE: JavaInterfaceBase.java

public interface JavaInterfaceBase {
    default String test() {
        return "OK";
    }

    default String testOverride() {
        return "OK";
    }
}

// FILE: JavaInterface.java

public interface JavaInterface extends JavaInterfaceBase {
    static String testStatic() {
        return "OK";
    }
}


// FILE: 1.kt
import JavaInterface.testStatic

interface KotlinInterface : JavaInterface {
    fun fooo() {
        <!INTERFACE_STATIC_METHOD_CALL_FROM_JAVA6_TARGET_ERROR!>testStatic<!>()
        super.<!INTERFACE_CANT_CALL_DEFAULT_METHOD_VIA_SUPER!>test<!>()
        test()
        testOverride()
    }

    override fun testOverride(): String {
        return "OK";
    }
}

interface KotlinInterfaceIndirectInheritance : KotlinInterface {
    fun foooo() {
        <!INTERFACE_STATIC_METHOD_CALL_FROM_JAVA6_TARGET_ERROR!>testStatic<!>()
        super.<!INTERFACE_CANT_CALL_DEFAULT_METHOD_VIA_SUPER!>test<!>()
        testOverride()
        super.testOverride()
    }
}

open class KotlinClass : JavaInterface {
    fun foo(){
        <!INTERFACE_STATIC_METHOD_CALL_FROM_JAVA6_TARGET_ERROR!>testStatic<!>()
        super.<!DEFAULT_METHOD_CALL_FROM_JAVA6_TARGET_ERROR!>test<!>()
        super.<!DEFAULT_METHOD_CALL_FROM_JAVA6_TARGET_ERROR!>testOverride<!>()
    }
}

class KotlinClassIndirectInheritance : KotlinClass() {
    fun foo2(){
        <!INTERFACE_STATIC_METHOD_CALL_FROM_JAVA6_TARGET_ERROR!>testStatic<!>()
        super.test()
        super.testOverride()
    }
}

class KotlinClassIndirectInheritance2 : KotlinInterfaceIndirectInheritance {
    fun foo(){
        <!INTERFACE_STATIC_METHOD_CALL_FROM_JAVA6_TARGET_ERROR!>testStatic<!>()
        super.<!DEFAULT_METHOD_CALL_FROM_JAVA6_TARGET_ERROR!>test<!>()
        super.testOverride()
    }
}

fun test() {
    JavaInterface.<!INTERFACE_STATIC_METHOD_CALL_FROM_JAVA6_TARGET_ERROR!>testStatic<!>()
    KotlinClass().foo()
    KotlinClassIndirectInheritance2().foo()

    KotlinClass().test()
    KotlinClass().testOverride()
    KotlinClassIndirectInheritance().testOverride()
}
