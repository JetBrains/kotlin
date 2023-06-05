// WITH_STDLIB
// IGNORE_BACKEND: JVM

annotation class KotlinAnn(vararg val foo: String)

annotation class KotlinIntAnn(vararg val foo: Int)

annotation class KotlinUIntAnn(vararg val foo: UInt)

fun box(): String {
    KotlinAnn()
    KotlinIntAnn()
    KotlinUIntAnn()
    return "OK"
}