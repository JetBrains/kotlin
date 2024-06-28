// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER

interface Foo

// isolated example when we have two canidates where one of them has DeferredType

fun Foo.bar(): Int = 0

fun call(f: Any) {}

val String.bar
    get() = call(Foo::bar)

// test from KT-39470

interface Bar {
    val serializationWhitelists: List<Foo>
}

val List<Bar>.serializationWhitelists
    get() = flatMapTo(LinkedHashSet(), Bar::serializationWhitelists)