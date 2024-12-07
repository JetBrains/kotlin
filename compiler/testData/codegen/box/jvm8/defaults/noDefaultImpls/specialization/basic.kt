// TARGET_BACKEND: JVM
// JVM_DEFAULT_MODE: all
// JVM_TARGET: 1.8
interface Foo<T> {
    fun test(p: T) = p
    val T.prop: T
        get() = this
}

open class BaseSpecialized : Foo<String> {

}

fun box(): String {
    val base = BaseSpecialized()
    return base.test("O") + with(base) { "K".prop }
}
