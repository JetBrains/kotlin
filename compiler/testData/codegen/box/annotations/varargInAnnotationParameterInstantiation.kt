annotation class KotlinAnn(vararg val foo: String)

annotation class KotlinIntAnn(vararg val foo: Int)
fun box(): String {
    KotlinAnn()
    KotlinIntAnn()
    return "OK"
}