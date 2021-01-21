// !JVM_TARGET: 1.6
// FILE: Test.java
public interface Test {
    default String test() {
        return "123";
    }
}

// FILE: test.kt
interface KTrait : Test {
    fun ktest() {
        super.<!INTERFACE_CANT_CALL_DEFAULT_METHOD_VIA_SUPER!>test<!>()

        test()
    }
}


interface KTrait2 : KTrait {
    fun ktest2() {
        super.<!INTERFACE_CANT_CALL_DEFAULT_METHOD_VIA_SUPER!>test<!>()

        test()
    }
}

class A : KTrait {
    fun a() {
        super.<!DEFAULT_METHOD_CALL_FROM_JAVA6_TARGET_ERROR!>test<!>()

        test()
    }
}


class A2 : KTrait2 {
    fun a() {
        super.<!DEFAULT_METHOD_CALL_FROM_JAVA6_TARGET_ERROR!>test<!>()

        test()
    }
}
