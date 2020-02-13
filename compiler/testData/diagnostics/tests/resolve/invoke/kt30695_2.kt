class MemberInvokeOwner {
    operator fun invoke() {}
}

class Cls {
    fun testImplicitReceiver() {
        <!UNSAFE_IMPLICIT_INVOKE_CALL!>nullableExtensionProperty<!>()
    }
}

val Cls.nullableExtensionProperty: MemberInvokeOwner?
    get() = null

val Cls.extensionProperty: MemberInvokeOwner
    get() = TODO()

fun testNullableReceiver(nullable: Cls?) {
    nullable?.<!UNSAFE_IMPLICIT_INVOKE_CALL!>extensionProperty<!>()
    nullable<!UNSAFE_CALL!>.<!><!FUNCTION_EXPECTED!>extensionProperty<!>()
}

fun testNotNullableReceiver(notNullable: Cls) {
    notNullable.<!UNSAFE_IMPLICIT_INVOKE_CALL!>nullableExtensionProperty<!>()
    notNullable<!UNNECESSARY_SAFE_CALL!>?.<!>extensionProperty()
}
