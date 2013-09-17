class C {
    fun foo(suppress("REDUNDANT_NULLABLE") p: String?? = null <!USELESS_CAST!>as<!> Nothing??) = p
}