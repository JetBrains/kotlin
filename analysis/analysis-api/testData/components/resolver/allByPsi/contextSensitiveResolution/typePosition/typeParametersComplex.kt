// ISSUE: KT-75061
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType

sealed class SealedGeneric<T> {
    class SGOption1<T>(val prop1: Int): SealedGeneric<T>()
    class SGOption2<T>(val prop2: Int): SealedGeneric<T>()
}
fun testTypeParams(instance1: SealedGeneric<*>, instance2: SealedGeneric<out CharSequence>): Int {
    if (instance1 is SGOption1<String>) {
        return instance1.prop1
    }
    if (instance1 is SGOption1<*>) {
        return instance1.prop1
    }
    if (instance2 is SGOption2<CharSequence>) {
        return instance2.prop2
    }
    if (instance2 is SGOption2<in CharSequence>) {
        return instance2.prop2
    }
    if (instance2 is SGOption2<out CharSequence>) {
        return instance2.prop2
    }
    return 0
}

fun <T>testTypeParams2(a: SealedGeneric<T>) {
    when(a) {
        is SGOption1<*> -> ""
        is SGOption2<*> -> ""
    }

    when(a) {
        is SGOption1<T> -> ""
        is SGOption2<T> -> ""
    }
}

fun testTypeParams2(a: SealedGeneric<String>, b: SealedGeneric<String> ) {
    when(a) {
        is SGOption1 -> ""
        is SGOption2 -> ""
    }

    b as SGOption1
}

fun testTypeParams3(a: SealedGeneric<*>, b: SealedGeneric<*>) {
    a as SGOption1<String>
    if (b is SGOption1<String>) {}
}
