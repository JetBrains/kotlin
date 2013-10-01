suppress("REDUNDANT_NULLABLE", "UNNECESSARY_NOT_NULL_ASSERTION")
object C {
    fun foo(): String?? = ""!! <!USELESS_CAST_STATIC_ASSERT_IS_FINE!>as<!> String??
}