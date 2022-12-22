// FIR_IDENTICAL
// WITH_STDLIB
// LANGUAGE: +CustomEqualsInValueClasses, +ValueClasses
// !DIAGNOSTICS: -INLINE_CLASS_DEPRECATED
// SKIP_TXT

@JvmInline
@AllowTypedEquals
value class IC1(val x: Int)

@JvmInline
@AllowTypedEquals
value class IC2(val x: Int, val y: String)

@AllowTypedEquals
inline class IC3(val x : Int)

<!INAPPLICABLE_ALLOW_TYPED_EQUALS_ANNOTATION!>@AllowTypedEquals<!>
class A