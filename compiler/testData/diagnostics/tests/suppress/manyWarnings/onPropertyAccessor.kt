class C {
    val foo: String?
        [suppress("REDUNDANT_NULLABLE", "UNNECESSARY_NOT_NULL_ASSERTION")]
        get(): String?? = ""!! <!USELESS_CAST_STATIC_ASSERT_IS_FINE!>as<!> String??
}