// IGNORE_BACKEND_FIR: JVM_IR
interface A<T> {
    val property : T

    open  fun a() : T {
        return property
    }
}

open class B : A<Any> {

    override val property: Any = "fail"
}

open class C : B(), A<Any> {

    override val property: Any = "OK"
}

fun box() : String {
    return C().a() as String
}