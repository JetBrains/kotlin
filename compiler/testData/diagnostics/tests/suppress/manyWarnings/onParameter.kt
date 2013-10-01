class C {
    fun foo(suppress("REDUNDANT_NULLABLE", "UNNECESSARY_NOT_NULL_ASSERTION") p: String?? = ""!! <!USELESS_CAST_STATIC_ASSERT_IS_FINE!>as<!> String??) = p
}