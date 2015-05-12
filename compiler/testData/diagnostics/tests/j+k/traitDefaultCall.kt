// FILE: Test.java
public interface Test {
    default String test() {
        return "123";
    }
}

// FILE: test.kt
interface KTrait : Test {
    fun ktest() {
        <!TRAIT_CANT_CALL_DEFAULT_METHOD_VIA_SUPER!>super.test()<!>

        test()
    }
}


class A : KTrait {
    fun a() {
        super.test()

        test()
    }
}
