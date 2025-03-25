// ISSUE: KT-76058
// LANGUAGE: +ResolveTopLevelLambdasAsSyntheticCallArgument

// MODULE: lib
// FILE: lib.kt

fun <E> execute(action: () -> E): E = action()
fun <T> materialize(): T = "O" as T

// MODULE: app(lib)
// FILE: main.kt

interface TypeVariableOwner<P> {
    var typeVariableProducer1: () -> P
    var typeVariableProducer2: () -> P
}

fun <T> pcla(lambda: (TypeVariableOwner<T>) -> Unit): String {
    val p = object : TypeVariableOwner<T> {
        override var typeVariableProducer1: () -> T = { null!! }
        override var typeVariableProducer2: () -> T = { null!! }
    }
    lambda(p)

    return (p.typeVariableProducer1.invoke() as String) + (p.typeVariableProducer2.invoke() as String)
}

fun box(): String {
    return pcla { typeVariableOwner ->
        typeVariableOwner.typeVariableProducer1 = { materialize() }
        typeVariableOwner.typeVariableProducer2 = { execute { "K" } }
    }
}
