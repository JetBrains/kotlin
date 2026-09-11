// CURIOUS_ABOUT: partiallyMatchingStub, partiallyMatchingStub$default
// ISSUE: KT-89206

// `value` is boxed in the stub and copied to a local; `fixed` can reuse the stub's slot.
// The defaulted `other` argument is not a plain parameter read, so it is still copied.

@Suppress("UNCHECKED_CAST")
inline fun <T : Long> partiallyMatchingStub(fixed: String, value: T = 42L as T, other: String = "a"): Long =
    value + fixed.length + other.length
