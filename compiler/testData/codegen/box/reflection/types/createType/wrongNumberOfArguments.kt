// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_REFLECT

import kotlin.reflect.full.createType
import kotlin.reflect.KClassifier
import kotlin.reflect.KTypeProjection

fun test(classifier: KClassifier, arguments: List<KTypeProjection>) {
    try {
        classifier.createType(arguments)
        throw AssertionError("createType should have thrown IllegalArgumentException")
    }
    catch (e: IllegalArgumentException) {
        // OK
    }
}

class Outer<O> {
    inner class Inner<I>
    class Nested<N>
}

fun box(): String {
    val p = KTypeProjection.STAR

    test(String::class, listOf(p))
    test(String::class, listOf(p, p))
    test(List::class, listOf())
    test(List::class, listOf(p, p))
    test(Map::class, listOf())
    test(Map::class, listOf(p))
    test(Map::class, listOf(p, p, p))
    test(Array<Any>::class, listOf())

    test(Outer::class, listOf())
    test(Outer::class, listOf(p, p))

    // Outer.Inner takes two arguments: first for O, second for I
    test(Outer.Inner::class, listOf())
    test(Outer.Inner::class, listOf(p))
    test(Outer.Inner::class, listOf(p, p, p))

    // Outer.Nested takes one argument for N
    test(Outer.Nested::class, listOf())
    test(Outer.Nested::class, listOf(p, p))

    test(Outer::class.typeParameters.single(), listOf(p))

    return "OK"
}
