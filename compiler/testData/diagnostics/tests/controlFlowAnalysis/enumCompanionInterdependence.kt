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

// From KT-11769
enum class Fruit(personal: Int) {
    APPLE(1);

    companion object {
        val common = 20
    }

    val score = personal + <!UNINITIALIZED_VARIABLE!>common<!>
}

// Another example from KT-11769
enum class EnumCompanion1(val x: Int) {
    INSTANCE(<!UNINITIALIZED_ENUM_COMPANION!>Companion<!>.foo()),
    ANOTHER(foo());

    companion object {
        fun foo() = 42
    }
}
// Also should be reported for implicit receiver
enum class EnumCompanion2(val x: Int) {
    INSTANCE(<!UNINITIALIZED_ENUM_COMPANION!>foo<!>());

    companion object {
        fun foo() = 42
    }
}
// But not for another enum
enum class EnumCompanion3(val x: Int) {
    INSTANCE(EnumCompanion1.foo()),
    ANOTHER(EnumCompanion2.foo());

    companion object
}
