// FILE: Test.java

public interface Test {
    default String test() {
        return "OK";
    }
}

// FILE: test.kt

interface KInterface : Test {

}

class KClass : Test {
    fun ktest(): String {
        return super.test() + test()
    }
}

class KTClass : KInterface {
    fun ktest(): String {
        return super.test() + test()
    }
}


fun box(): String {
    val p = object : KInterface {
        fun ktest(): String {
            return super.test() + test()
        }
    }.ktest()

    if (p != "OKOK") return "fail1: $p"

    if (KClass().ktest() != "OKOK") return "fail 2: ${KClass().ktest()}"

    if (KTClass().ktest() != "OKOK") return "fail 3: ${KTClass().ktest()}"

    return "OK"
}
