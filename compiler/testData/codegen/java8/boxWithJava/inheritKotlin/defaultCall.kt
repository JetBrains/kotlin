trait KTrait {
    fun test(): String {
        return "base";
    }
}

class Test : Simple {

    fun bar(): String {
        return super.test()
    }

}

fun box(): String {
    val test = Test().test()
    if (test != "simple") return "fail $test"

    val bar = Test().bar()
    if (bar != "simple") return "fail 2 $bar"

    return "OK"
}