// CHECK_BYTECODE_TEXT
// 0 java/lang/invoke/LambdaMetafactory

interface Top
interface Unrelated

interface A : Top, Unrelated
interface B : Top, Unrelated

fun box(): String {
    val g = when ("".length) {
        0 -> G<A>()
        else -> G<B>()
    }

    g.check {}
    g.check(::functionReference)
    return "OK"
}

fun functionReference(x: Any) {}

class G<T : Top> {
    fun check(x: IFoo<in T>) {
        x.accept(object : A {} as T)
    }
}

fun interface IFoo<T : Top> {
    fun accept(t: T)
}
