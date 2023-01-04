external enum class E {
    X,
    <!EXTERNAL_ENUM_ENTRY_WITH_BODY!>Y {
        fun foo()
    },<!>
    <!EXTERNAL_ENUM_ENTRY_WITH_BODY!>Z {}<!>
}
