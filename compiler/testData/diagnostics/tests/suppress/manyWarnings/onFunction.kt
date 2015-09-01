class C {
    @Suppress("REDUNDANT_NULLABLE", "UNNECESSARY_NOT_NULL_ASSERTION")
    fun foo(): String?? = ""!! <!USELESS_CAST!>as String??<!>
}