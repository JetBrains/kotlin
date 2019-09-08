// IGNORE_BACKEND: JS_IR, JS, NATIVE
// WITH_REFLECT

interface A1 {
    fun test(o: String, k: String): String
}
interface A2 : A1

interface B1 {
    fun test(o: String = "O", k: String = "K"): String
}
interface B2 : B1

interface C1
interface C2 : C1

interface D
interface E : A2, B2, C2 {
    override fun test(o: String, k: String): String {
        return o + k
    }
}

class Z : D, E

fun box(): String {
    val f = Z::test
    return f.callBy(mapOf(f.parameters.first() to Z()))
}
