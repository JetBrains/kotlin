enum class A(val v: A) {
    A1(<!UNINITIALIZED_ENUM_ENTRY!>A2<!>),
    A2(A1)
}

enum class B(val x: Int) {
    B1(1),
    B2(2);

    companion object {
        val SUM = B1.x + B2.x
        val COPY = B1
    }
}

enum class C(val x: Int) {
    C1(<!UNINITIALIZED_VARIABLE!>SUM<!>),
    C2(1);

    companion object {
        val COPY = C2
        val SUM = C1.x + COPY.x
    }
}

enum class D(val x: Int) {
    D1(<!UNINITIALIZED_ENUM_ENTRY!>D2<!>.x),
    D2(D1.x)
}

enum class E(val v: Int) {
    // KT-11769 related: there is no predictable initialization order for enum entry with non-companion object
    E1(Nested.COPY);

    object Nested {
        val COPY = E1.v
    }
}
// From KT-13322: cross reference should not be reported here
object Object1 {
    val y: Any = Object2.z
    object Object2 {
        val z: Any = Object1.y
    }
}

// From KT-11769
enum class Fruit(personal: Int) {
    APPLE(1);

    companion object {
        val common = 20
    }

    val score = personal + <!UNINITIALIZED_VARIABLE!>common<!>
}

// From KT-6054
enum class MyEnum {
    A, B;
    val x = when(<!DEBUG_INFO_LEAKING_THIS!>this<!>) {
        <!UNINITIALIZED_ENUM_ENTRY!>A<!> -> 1
        <!UNINITIALIZED_ENUM_ENTRY!>B<!> -> 2
        else -> 3
    }
}
