// ISSUE: KT-89206
// WITH_STDLIB

// Pins `$default` descriptors for primitive upper-bound type parameters.

package test

@JvmInline
value class IntWrapper(val value: Int)

@JvmInline
value class StringWrapper(val value: String)

@Suppress("UNCHECKED_CAST")
inline fun <T : Long> inlineLongBound(value: T = 42L as T): Long = value

@Suppress("UNCHECKED_CAST")
fun <T : Long> longBound(value: T = 42L as T): Long = value

@Suppress("UNCHECKED_CAST")
fun <T : IntWrapper> valueClassBound(value: T = IntWrapper(42) as T): Int = value.value

// A reference-backed value class keeps the same JVM type in the stub and body.
@Suppress("UNCHECKED_CAST")
fun <T : StringWrapper> referenceValueClassBound(value: T = StringWrapper("42") as T): String = value.value

// Only `value` differs between the stub and body; `other` is a `String` in both.
@Suppress("UNCHECKED_CAST")
inline fun <T : Long> partiallyMatchingStub(value: T = 42L as T, other: String = "a"): String = "$value$other"

@Suppress("UNCHECKED_CAST")
fun <T : Long> nullableLongBound(value: T? = 42L as T): Long? = value

fun <T : CharSequence> referenceBound(value: T? = null): T? = value
