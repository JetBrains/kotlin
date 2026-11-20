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
value class NestedNullable(val inner: NotNullRefUnderlying?)

@JvmInline
value class NestedBoxed(val inner: PrimitiveUnderlying?)

@JvmInline
value class Generic<T>(val t: T)

@JvmInline
value class GenericBounded<T : CharSequence>(val t: T)

// A value class cannot be recursive through its constructor parameter (`value class A(val b: B)` together with
// `value class B(val a: A?)` is rejected by the frontend). The closest legal shapes are recursion through a type
// argument at the use site, through a non-inline indirection, and through a chain of bounds.
@JvmInline
value class Node(val next: List<Node>?)

@JvmInline
value class BoundedByValueClass<T : PrimitiveUnderlying?>(val t: T)

@JvmInline
value class BoundedByBounded<T : BoundedByValueClass<*>?>(val t: T)

@JvmInline
value class BoundedByNonNullValueClass<T : PrimitiveUnderlying>(val t: T)

@JvmInline
value class BoundedByNullableString<T : String?>(val t: T)

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

// A non-null `NestedNullable` is a `Ljava/lang/String;` slot which can still hold `null`, standing for
// `NestedNullable(null)`. `NestedNullable?` has to be boxed, since that `null` is already taken.
fun nestedNullable(nullable: NestedNullable?, notNull: NestedNullable) =
    (nullable?.inner?.s ?: "") + (notNull.inner?.s ?: "")

// A non-null `NestedBoxed` is a `LPrimitiveUnderlying;` slot: the underlying value is itself a boxed inline class.
// The name still encodes the outer class, since the slot holds its underlying value.
fun nestedBoxed(nullable: NestedBoxed?, notNull: NestedBoxed) =
    (nullable?.inner?.i ?: 0) + (notNull.inner?.i ?: 0)

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

// `Generic<Generic<String>>` is not a recursive class, only a nested type: `T`'s bound `Any?` still decides,
// so the nullable slot is boxed and the non-null one is a `Ljava/lang/Object;` holding the inner instance.
fun viaTypeArgument(nullable: Generic<Generic<String>>?, notNull: Generic<Generic<String>>) =
    (nullable?.t?.t ?: "") + notNull.t.t

// `Node` refers to itself only through `List<Node>?`, which is a nullable non-inline type: `Node?` is boxed,
// non-null `Node` is a `Ljava/util/List;` slot (possibly `null`, standing for `Node(null)`).
fun viaIndirection(nullable: Node?, notNull: Node) =
    (nullable?.next?.size ?: 0) + (notNull.next?.size ?: 0)

// The bound is a nullable value class over a primitive, so the underlying value is a boxed `PrimitiveUnderlying`:
// non-null `BoundedByValueClass<PrimitiveUnderlying?>` is a `LPrimitiveUnderlying;` slot, the nullable one is boxed.
// This is the generic counterpart of `nestedBoxed`.
fun boundedByValueClass(nullable: BoundedByValueClass<PrimitiveUnderlying?>?, notNull: BoundedByValueClass<PrimitiveUnderlying?>) =
    (nullable?.t?.i ?: 0) + (notNull.t?.i ?: 0)

// One level further: the underlying value of `BoundedByBounded<...>` is a boxed `BoundedByValueClass`.
fun boundedByBounded(
    nullable: BoundedByBounded<BoundedByValueClass<PrimitiveUnderlying?>?>?,
    notNull: BoundedByBounded<BoundedByValueClass<PrimitiveUnderlying?>?>,
) = (nullable?.t?.t?.i ?: 0) + (notNull.t?.t?.i ?: 0)

// A non-null value class bound expands all the way to `int`, so `BoundedByNonNullValueClass<PrimitiveUnderlying>?`
// has to be boxed.
fun boundedByNonNullValueClass(nullable: BoundedByNonNullValueClass<PrimitiveUnderlying>?, notNull: BoundedByNonNullValueClass<PrimitiveUnderlying>) =
    (nullable?.t?.i ?: 0) + notNull.t.i

// A nullable reference bound makes the non-null type a nullable `Ljava/lang/String;` slot and the nullable type boxed,
// no matter whether the argument itself is nullable.
fun boundedByNullableString(nullable: BoundedByNullableString<String?>?, notNull: BoundedByNullableString<String?>) =
    (nullable?.t ?: "") + (notNull.t ?: "")

fun boundedByNullableStringNotNullArgument(nullable: BoundedByNullableString<String>?, notNull: BoundedByNullableString<String>) =
    (nullable?.t ?: "") + notNull.t

private fun check(methodNamePrefix: String, expectedNullableName: String, expectedNotNullName: String): String? {
    // All of these methods are mangled, so the prefix is followed by the hash separator.
    val method = Class.forName("AKt").declaredMethods.single { it.name.startsWith("$methodNamePrefix-") }
    val parameters = method.getParameters()
    if (parameters[0].name != expectedNullableName)
        return "wrong name on nullable parameter of $methodNamePrefix: ${parameters[0].name}"
    if (parameters[1].name != expectedNotNullName)
        return "wrong name on non-null parameter of $methodNamePrefix: ${parameters[1].name}"
    return null
}

fun box(): String {
    // Boxed nullable slots keep the plain name.
    check("primitive", "nullable", "\$v\$c\$PrimitiveUnderlying\$\$notNull")?.let { return it }
    check("nullableRef", "nullable", "\$v\$c\$NullableRefUnderlying\$\$notNull")?.let { return it }
    check("genericUnbounded", "nullable", "\$v\$c\$Generic\$\$notNull")?.let { return it }
    check("typeParameter", "nullable", "\$v\$c\$PrimitiveUnderlying\$\$notNull")?.let { return it }
    check("nestedNullable", "nullable", "\$v\$c\$NestedNullable\$\$notNull")?.let { return it }
    check("nestedBoxed", "nullable", "\$v\$c\$NestedBoxed\$\$notNull")?.let { return it }
    check("viaTypeArgument", "nullable", "\$v\$c\$Generic\$\$notNull")?.let { return it }
    check("viaIndirection", "nullable", "\$v\$c\$Node\$\$notNull")?.let { return it }
    check("boundedByValueClass", "nullable", "\$v\$c\$BoundedByValueClass\$\$notNull")?.let { return it }
    check("boundedByBounded", "nullable", "\$v\$c\$BoundedByBounded\$\$notNull")?.let { return it }
    check("boundedByNonNullValueClass", "nullable", "\$v\$c\$BoundedByNonNullValueClass\$\$notNull")?.let { return it }
    check("boundedByNullableString", "nullable", "\$v\$c\$BoundedByNullableString\$\$notNull")?.let { return it }
    check("boundedByNullableStringNotNullArgument", "nullable", "\$v\$c\$BoundedByNullableString\$\$notNull")?.let { return it }

    // Unboxed nullable slots are encoded like non-null ones. A `null` in such a slot is a `null` value class instance,
    // whereas a `null` in a slot of a *non-null* type over a nullable underlying type (`nestedNullable`, `nestedBoxed`
    // above) is an instance wrapping `null`; the two cannot occur for the same value class.
    check("notNullRef", "\$v\$c\$NotNullRefUnderlying\$\$nullable", "\$v\$c\$NotNullRefUnderlying\$\$notNull")?.let { return it }
    check("nested", "\$v\$c\$Nested\$\$nullable", "\$v\$c\$Nested\$\$notNull")?.let { return it }
    check("genericBounded", "\$v\$c\$GenericBounded\$\$nullable", "\$v\$c\$GenericBounded\$\$notNull")?.let { return it }

    return "OK"
}
