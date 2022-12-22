// FIR_IDENTICAL
// WITH_STDLIB
// !DIAGNOSTICS: -DEBUG_INFO_SMARTCAST -EXTENSION_SHADOWED_BY_MEMBER
// LANGUAGE: +CustomEqualsInValueClasses
// SKIP_TXT

@JvmInline
@AllowTypedEquals
value class IC1(val x: Int) {
    override fun equals(other: Any?) = true

    @TypedEquals
    fun equals(other: IC1) = true

    override fun hashCode() = 0
}

@JvmInline
@OptIn(AllowTypedEquals::class)
value class IC2(val x: Int) {
    <!INAPPLICABLE_TYPED_EQUALS_ANNOTATION!>@TypedEquals<!>
    fun equals(other: IC1) = true

    <!INAPPLICABLE_TYPED_EQUALS_ANNOTATION!>@TypedEquals<!>
    fun equals(other: IC2) {
    }
}

@JvmInline
@AllowTypedEquals
value class IC3<T>(val x: T) {
    <!INAPPLICABLE_TYPED_EQUALS_ANNOTATION!>@TypedEquals<!>
    fun equals(other: IC3<T>) = true
}

@JvmInline
@AllowTypedEquals
value class IC4<T>(val x: T) {
    <!INAPPLICABLE_TYPED_EQUALS_ANNOTATION!>@TypedEquals<!>
    fun equals(other: IC4<String>) = true
}

@JvmInline
@AllowTypedEquals
value class IC5<T : Number>(val x: T) {
    <!INAPPLICABLE_TYPED_EQUALS_ANNOTATION!>@TypedEquals<!>
    fun equals(other: T) = true
}

@JvmInline
@AllowTypedEquals
value class IC6<T, R>(val x: T) {
    @TypedEquals
    fun <!TYPE_PARAMETERS_NOT_ALLOWED!><S1, S2><!> equals(other: IC6<*, *>) = true
}

@JvmInline
@AllowTypedEquals
value class IC7<T, R>(val x: T) {
    @TypedEquals
    fun equals(other: IC7<*, *>) = true
}

@JvmInline
@AllowTypedEquals
value class IC8<T, R>(val x: T) {
    @TypedEquals
    fun equals(other: IC8<*, *>): Boolean = TODO()
}

@JvmInline
value class IC9<T, R>(val x: T) {
    <!INAPPLICABLE_TYPED_EQUALS_ANNOTATION!>@TypedEquals<!>
    fun equals(other: IC9<*, *>) = true
}

@JvmInline
@AllowTypedEquals
value class IC10(val x: Int)

@OptIn(AllowTypedEquals::class)
<!INAPPLICABLE_TYPED_EQUALS_ANNOTATION!>@TypedEquals<!>
fun IC10.equals(other: IC10) = true

@JvmInline
@AllowTypedEquals
value class IC11(val x: Int) {
    <!INAPPLICABLE_TYPED_EQUALS_ANNOTATION!>@TypedEquals<!>
    fun equals(other: IC11?) = true
}

@OptIn(AllowTypedEquals::class)
<!INAPPLICABLE_TYPED_EQUALS_ANNOTATION!>@TypedEquals<!>
fun equals(left : IC11, right: IC11) = true