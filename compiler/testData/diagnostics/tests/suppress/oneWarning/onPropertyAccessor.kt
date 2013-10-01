class C {
    val foo: String?
        [suppress("REDUNDANT_NULLABLE")]
        get(): String?? = null <!USELESS_CAST!>as<!> Nothing??
}