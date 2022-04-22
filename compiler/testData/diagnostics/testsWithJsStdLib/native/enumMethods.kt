enum class Base {
    X,
    Y;
}

external enum class A {
    X,
    Y;
}

external enum class B {
    X,
    Y;

    @Suppress("VIRTUAL_MEMBER_HIDDEN")
    val name: String
}

external enum class C {
    X,
    Y;

    companion object {
        fun values(): Array<C>
    }
}

fun main() {
    // Regular enum
    Base.values()
    Base.valueOf("X")
    enumValues<Base>()
    enumValueOf<Base>("Y")
    Base.X.name
    Base.Y.ordinal
    Base.X.compareTo(Base.Y)
    Base.X.equals(Base.Y)
    Base.Y.hashCode()
    Base.Y.toString()

    // Raw external enum
    A.<!ENUM_SYNTHETIC_METHOD_USAGE_WITH_EXTERNAL_ENUM!>values<!>()
    A.<!ENUM_SYNTHETIC_METHOD_USAGE_WITH_EXTERNAL_ENUM!>valueOf<!>("X")
    <!ENUM_STDLIB_HELPERS_USAGE_WITH_EXTERNAL_ENUM!>enumValues<!><A>()
    <!ENUM_STDLIB_HELPERS_USAGE_WITH_EXTERNAL_ENUM!>enumValueOf<!><A>("Y")
    A.X.<!ENUM_SYNTHETIC_METHOD_USAGE_WITH_EXTERNAL_ENUM!>name<!>
    A.Y.<!ENUM_SYNTHETIC_METHOD_USAGE_WITH_EXTERNAL_ENUM!>ordinal<!>
    A.X.compareTo(A.Y)
    A.X.equals(A.Y)
    A.Y.hashCode()
    A.Y.toString()

    // External enum with hidden "name"
    B.<!ENUM_SYNTHETIC_METHOD_USAGE_WITH_EXTERNAL_ENUM!>values<!>()
    B.<!ENUM_SYNTHETIC_METHOD_USAGE_WITH_EXTERNAL_ENUM!>valueOf<!>("X")
    <!ENUM_STDLIB_HELPERS_USAGE_WITH_EXTERNAL_ENUM!>enumValues<!><B>()
    <!ENUM_STDLIB_HELPERS_USAGE_WITH_EXTERNAL_ENUM!>enumValueOf<!><B>("Y")
    B.X.name
    B.Y.<!ENUM_SYNTHETIC_METHOD_USAGE_WITH_EXTERNAL_ENUM!>ordinal<!>
    B.X.compareTo(B.Y)
    B.X.equals(B.Y)
    B.Y.hashCode()
    B.Y.toString()

    // External enum with hidden "values"
    // There is a problem with C.values
    C.Companion.values()
    C.<!ENUM_SYNTHETIC_METHOD_USAGE_WITH_EXTERNAL_ENUM!>values<!>()
    C.<!ENUM_SYNTHETIC_METHOD_USAGE_WITH_EXTERNAL_ENUM!>valueOf<!>("X")
    <!ENUM_STDLIB_HELPERS_USAGE_WITH_EXTERNAL_ENUM!>enumValues<!><C>()
    <!ENUM_STDLIB_HELPERS_USAGE_WITH_EXTERNAL_ENUM!>enumValueOf<!><C>("Y")
    C.X.<!ENUM_SYNTHETIC_METHOD_USAGE_WITH_EXTERNAL_ENUM!>name<!>
    C.Y.<!ENUM_SYNTHETIC_METHOD_USAGE_WITH_EXTERNAL_ENUM!>ordinal<!>
    C.X.compareTo(C.Y)
    C.X.equals(C.Y)
    C.Y.hashCode()
    C.Y.toString()
}
