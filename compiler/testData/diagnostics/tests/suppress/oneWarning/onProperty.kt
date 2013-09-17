class C {
    suppress("REDUNDANT_NULLABLE")
    val foo: String?? = null <!USELESS_CAST!>as<!> Nothing?
}