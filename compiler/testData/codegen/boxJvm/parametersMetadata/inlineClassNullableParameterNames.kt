// SKIP_JDK6
// TARGET_BACKEND: JVM
// WITH_STDLIB
// FULL_JDK
// PARAMETERS_METADATA

// A parameter gets a value class-encoded name exactly when its JVM slot holds the underlying value, and keeps its
// plain name when the slot holds a boxed instance. For nullable value class types this depends on the underlying type:
// primitive or nullable underlying types are boxed, since `null` could not be represented otherwise, whereas a
// non-null reference underlying type is stored as a nullable reference. See `addOrInheritInlineClassPropertyNameParts`.

// FILE: A.kt

@JvmInline
value class PrimitiveUnderlying(val i: Int)

@JvmInline
value class NullableRefUnderlying(val s: String?)

@JvmInline
value class NotNullRefUnderlying(val s: String)

@JvmInline
value class Nested(val inner: NotNullRefUnderlying)

@JvmInline
value class Generic<T>(val t: T)

@JvmInline
value class GenericBounded<T : CharSequence>(val t: T)

// `PrimitiveUnderlying?` is boxed to `LPrimitiveUnderlying;`.
fun primitive(nullable: PrimitiveUnderlying?, notNull: PrimitiveUnderlying) =
    (nullable?.i ?: 0) + notNull.i

// `NullableRefUnderlying?` is boxed to `LNullableRefUnderlying;`: a bare `String?` could not tell
// `null` apart from `NullableRefUnderlying(null)`.
fun nullableRef(nullable: NullableRefUnderlying?, notNull: NullableRefUnderlying) =
    (nullable?.s ?: "") + (notNull.s ?: "")

// `NotNullRefUnderlying?` is not boxed: both slots are `Ljava/lang/String;`.
fun notNullRef(nullable: NotNullRefUnderlying?, notNull: NotNullRefUnderlying) =
    (nullable?.s ?: "") + notNull.s

// The expansion goes all the way down, so `Nested?` is a `Ljava/lang/String;` slot as well.
fun nested(nullable: Nested?, notNull: Nested) =
    (nullable?.inner?.s ?: "") + notNull.inner.s

// For a generic value class the upper bound of the type parameter decides, not the type argument:
// `Generic<String>?` is boxed since `T`'s bound is `Any?`, while `GenericBounded<String>?` is a
// `Ljava/lang/CharSequence;` slot.
fun genericUnbounded(nullable: Generic<String>?, notNull: Generic<String>) =
    (nullable?.t ?: "") + notNull.t

fun genericBounded(nullable: GenericBounded<String>?, notNull: GenericBounded<String>) =
    (nullable?.t ?: "") + notNull.t

// A type parameter bounded by a value class follows the bound: `T` is an `int` slot, `T?` is boxed.
fun <T : PrimitiveUnderlying> typeParameter(nullable: T?, notNull: T) =
    (nullable?.i ?: 0) + notNull.i

private fun check(methodNamePrefix: String, expectedNullableName: String, expectedNotNullName: String): String? {
    val method = Class.forName("AKt").declaredMethods.single { it.name.startsWith(methodNamePrefix) }
    val parameters = method.getParameters()
    if (parameters[0].name != expectedNullableName)
        return "wrong name on nullable parameter of $methodNamePrefix: ${parameters[0].name}"
    if (parameters[1].name != expectedNotNullName)
        return "wrong name on non-null parameter of $methodNamePrefix: ${parameters[1].name}"
    return null
}

fun box(): String {
    // Boxed nullable slots keep the plain name.
    check("primitive", "nullable", "\$v\$c\$PrimitiveUnderlying\$-notNull")?.let { return it }
    check("nullableRef", "nullable", "\$v\$c\$NullableRefUnderlying\$-notNull")?.let { return it }
    check("genericUnbounded", "nullable", "\$v\$c\$Generic\$-notNull")?.let { return it }
    check("typeParameter", "nullable", "\$v\$c\$PrimitiveUnderlying\$-notNull")?.let { return it }

    // Unboxed nullable slots are encoded like non-null ones; a `null` in such a slot is a `null` value class instance.
    check("notNullRef", "\$v\$c\$NotNullRefUnderlying\$-nullable", "\$v\$c\$NotNullRefUnderlying\$-notNull")?.let { return it }
    check("nested", "\$v\$c\$Nested\$-nullable", "\$v\$c\$Nested\$-notNull")?.let { return it }
    check("genericBounded", "\$v\$c\$GenericBounded\$-nullable", "\$v\$c\$GenericBounded\$-notNull")?.let { return it }

    return "OK"
}
