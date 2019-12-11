// FILE: test.kt

public interface Test {
    fun test(): String {
        return "123";
    }
}

interface KTrait : Test {
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

