class C {
    fun foo(): Any? {
        [suppress("REDUNDANT_NULLABLE")]
        val v: String?? = null <!USELESS_CAST!>as<!> Nothing??
        return v
    }
}