trait KTrait: Simple {
    override fun test(s: String): String {
        return s + "K"
    }
}

class Test : KTrait {

}

fun box(): String {
    return Test().test("O")
}