// SKIP_JDK6
// TARGET_BACKEND: JVM
// WITH_STDLIB
// FULL_JDK
// PARAMETERS_METADATA

// Whether a nullable value class parameter keeps its plain name depends on whether the JVM slot ends up
// holding a boxed instance or the underlying value, which in turn depends on the underlying type.
// See `addOrInheritInlineClassPropertyNameParts`.

// FILE: A.kt

@JvmInline
value class PrimitiveUnderlying(val i: Int)

@JvmInline
value class NullableRefUnderlying(val s: String?)

@JvmInline
value class NotNullRefUnderlying(val s: String)

// `PrimitiveUnderlying?` is boxed to `LPrimitiveUnderlying;`, since `int` cannot hold null.
fun primitive(nullable: PrimitiveUnderlying?, notNull: PrimitiveUnderlying) =
    (nullable?.i ?: 0) + notNull.i

// `NullableRefUnderlying?` is boxed to `LNullableRefUnderlying;`, since a bare `String?` could not tell
// `null` apart from `NullableRefUnderlying(null)`.
fun nullableRef(nullable: NullableRefUnderlying?, notNull: NullableRefUnderlying) =
    (nullable?.s ?: "") + (notNull.s ?: "")

// `NotNullRefUnderlying?` is *not* boxed: both parameters are `Ljava/lang/String;`.
fun notNullRef(nullable: NotNullRefUnderlying?, notNull: NotNullRefUnderlying) =
    (nullable?.s ?: "") + notNull.s

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
    // Boxed: the slot holds a real value class instance, so the name needs no encoding.
    check("primitive", "nullable", "\$v\$c\$PrimitiveUnderlying\$-notNull")?.let { return it }
    check("nullableRef", "nullable", "\$v\$c\$NullableRefUnderlying\$-notNull")?.let { return it }

    // Unboxed: the slot holds the underlying string. The name is currently left alone all the same,
    // because the encoding is skipped for every nullable type.
    check("notNullRef", "nullable", "\$v\$c\$NotNullRefUnderlying\$-notNull")?.let { return it }

    return "OK"
}
