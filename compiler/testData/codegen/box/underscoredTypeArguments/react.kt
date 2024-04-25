// LANGUAGE: +PartiallySpecifiedTypeArguments

interface RProps
open class RComponent<K, T> : Component<K, T>
interface RState
interface Component<K1, K2>
class RElementBuilder<A>
interface ReactElement

class RBuilder

interface MyProps<T> : RProps {
    var list: List<T>
}

class MyComponent<T> : RComponent<MyProps<T>, RState>() {}

inline fun <P : RProps, reified C : Component<P, *>> child(
    noinline handler: RElementBuilder<P>.() -> Unit
): String = "OK"

fun box(): String {
    child<MyProps<RBuilder.(String) -> Unit>, _> {

    }
    return child<_, MyComponent<RBuilder.(String) -> Unit>> {

    }
}
