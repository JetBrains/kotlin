// FIR_IDENTICAL
// WITH_STDLIB
import kotlin.reflect.KMutableProperty0

class Controller<T>

fun <S1> generate(g: suspend Controller<S1>.() -> Unit): S1 = TODO()

interface ReadonlyPropertyKey<V1, in K1>

fun <V2 : Any, K2> Controller<K2>.implement(
    propertyKey: ReadonlyPropertyKey<V2, K2>,
    getter: () -> V2,
) {
}

fun <V3 : Any, K3> Controller<K3>.implement(
    propertyKey: ReadonlyPropertyKey<V3, K3>,
    prop: KMutableProperty0<V3?>,
) {
}

interface Screen {
    val screen: Any? get() = ""
}

val Name: ReadonlyPropertyKey<String, Screen> = TODO()

val Screen.name: String
    get() = ""

class A : Screen {
    fun foo() {
        generate {
            implement(Name, ::name)
        }.screen
    }
}