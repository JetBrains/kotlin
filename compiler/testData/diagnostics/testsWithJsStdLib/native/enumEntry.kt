external enum class E {
    X,
    Y <!EXTERNAL_ENUM_ENTRY_WITH_BODY!>{
        fun foo()
    }<!>,
    Z <!EXTERNAL_ENUM_ENTRY_WITH_BODY!>{}<!>
}