enum class E {
    FIRST,

    SECOND;

    companion object {
        class FIRST

        val SECOND = <!DEBUG_INFO_LEAKING_THIS!>this<!>
    }
}