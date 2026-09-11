// ISSUE: KT-89206
// WORKS_WHEN_VALUE_CLASS
// WITH_STDLIB

// A primitive upper-bound type parameter is boxed in a `$default` stub but unboxed in the function body.

OPTIONAL_JVM_INLINE_ANNOTATION
value class IntWrapper(val value: Int)

OPTIONAL_JVM_INLINE_ANNOTATION
value class StringWrapper(val value: String)

@Suppress("UNCHECKED_CAST")
inline fun <T : Long> longBound(value: T = 42L as T): Long = value

@Suppress("UNCHECKED_CAST")
inline fun <T : Int> intBound(value: T = 42 as T): Int = value

@Suppress("UNCHECKED_CAST")
inline fun <T : IntWrapper> valueClassBound(value: T = IntWrapper(42) as T): Int = value.value

// A reference-backed value class still has the same JVM type in the stub and body.
@Suppress("UNCHECKED_CAST")
inline fun <T : StringWrapper> referenceValueClassBound(value: T = StringWrapper("42") as T): String = value.value

inline fun referenceValueClassBoundThroughStub(): String = referenceValueClassBound<StringWrapper>()

// Only `value` is boxed in the stub; `other` can still reuse the stub's slot.
@Suppress("UNCHECKED_CAST")
inline fun <T : Long> partiallyMatchingStub(value: T = 42L as T, other: String = "a"): String = "$value$other"

inline fun partiallyMatchingStubThroughStub(): String = partiallyMatchingStub<Long>()

@Suppress("UNCHECKED_CAST")
inline fun <T : Long> nullableLongBound(value: T? = 42L as T): Long? = value

// The exact shape reported in KT-89206: a `Nothing`-typed default. Declaring it used to be enough to crash codegen.
inline fun <T : Long> todoDefault(value: T = TODO()): Long = value

// Same, never called: the `$default` stub is generated for the declaration alone.
inline fun <T : Long> todoDefaultNeverCalled(value: T = TODO()) {
    value
}

fun box(): String {
    if (longBound<Long>() != 42L) return "Fail 1: ${longBound<Long>()}"
    if (longBound(1L) != 1L) return "Fail 2: ${longBound(1L)}"

    if (intBound<Int>() != 42) return "Fail 3: ${intBound<Int>()}"
    if (intBound(1) != 1) return "Fail 4: ${intBound(1)}"

    if (valueClassBound<IntWrapper>() != 42) return "Fail 5: ${valueClassBound<IntWrapper>()}"
    if (valueClassBound(IntWrapper(1)) != 1) return "Fail 6: ${valueClassBound(IntWrapper(1))}"

    if (referenceValueClassBound<StringWrapper>() != "42") return "Fail 7: ${referenceValueClassBound<StringWrapper>()}"
    if (referenceValueClassBound(StringWrapper("1")) != "1") return "Fail 8: ${referenceValueClassBound(StringWrapper("1"))}"
    if (referenceValueClassBoundThroughStub() != "42") return "Fail 9: ${referenceValueClassBoundThroughStub()}"

    if (partiallyMatchingStub<Long>() != "42a") return "Fail 10: ${partiallyMatchingStub<Long>()}"
    if (partiallyMatchingStub(1L) != "1a") return "Fail 11: ${partiallyMatchingStub(1L)}"
    if (partiallyMatchingStub(1L, "b") != "1b") return "Fail 12: ${partiallyMatchingStub(1L, "b")}"
    if (partiallyMatchingStubThroughStub() != "42a") return "Fail 13: ${partiallyMatchingStubThroughStub()}"

    if (nullableLongBound<Long>() != 42L) return "Fail 14: ${nullableLongBound<Long>()}"
    if (nullableLongBound(1L) != 1L) return "Fail 15: ${nullableLongBound(1L)}"

    if (todoDefault(7L) != 7L) return "Fail 16: ${todoDefault(7L)}"

    return "OK"
}
