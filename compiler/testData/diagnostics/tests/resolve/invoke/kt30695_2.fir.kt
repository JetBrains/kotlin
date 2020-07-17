// FILE: JavaClass.java

public class JavaClass {
    public static Cls createFlexible() {
        return new Cls();
    }
}

// FILE: test.kt

class MemberInvokeOwner {
    operator fun invoke() {}
}

class Cls {
    fun testImplicitReceiver() {
        <!INAPPLICABLE_CANDIDATE!>nullableExtensionProperty<!>()
    }
}

val Cls.nullableExtensionProperty: MemberInvokeOwner?
    get() = null

val Cls.extensionProperty: MemberInvokeOwner
    get() = TODO()

fun testNullableReceiver(nullable: Cls?) {
    nullable?.extensionProperty()
    nullable.<!UNRESOLVED_REFERENCE!>extensionProperty<!>()
}

fun testNotNullableReceiver(notNullable: Cls) {
    notNullable.<!INAPPLICABLE_CANDIDATE!>nullableExtensionProperty<!>()
    notNullable?.extensionProperty()
}

fun testFlexibleReceiver() {
    val flexible = JavaClass.createFlexible()
    flexible.extensionProperty()
    flexible?.extensionProperty()
    flexible.<!INAPPLICABLE_CANDIDATE!>nullableExtensionProperty<!>()
    flexible?.<!INAPPLICABLE_CANDIDATE!>nullableExtensionProperty<!>()
}
