// FILE: test.kt

public trait Test {
    fun test(): String {
        return "123";
    }
}

trait KTrait : Test {
    fun ktest() {
        super.test()

        test()
    }
}

class A : KTrait {
    fun b() {
        super.test()

        test()
    }
}

