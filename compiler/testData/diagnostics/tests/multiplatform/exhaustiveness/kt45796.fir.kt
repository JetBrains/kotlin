// ISSUE: KT-45796

// MODULE: m1-common
expect sealed class SealedClass() {
    class Nested : SealedClass {
        class NestedDeeper : SealedClass
    }
}

fun whenForExpectSealed(s: SealedClass): Int {
    return when (s) { // should be error, because actual sealed class may add more implementations
        is SealedClass.Nested.NestedDeeper -> 7
        is SealedClass.Nested -> 8
    }
}

// MODULE: m1-jvm()()(m1-common)
actual sealed class SealedClass {
    actual class Nested : SealedClass() {
        actual class NestedDeeper : SealedClass()
    }
}

fun whenForSealed(s: SealedClass): Int {
    return when (s) { // Should be OK
        is SealedClass.Nested.NestedDeeper -> 7
        is SealedClass.Nested -> 8
    }
}
