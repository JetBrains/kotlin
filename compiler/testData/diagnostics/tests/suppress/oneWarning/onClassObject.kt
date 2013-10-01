class C {
    suppress("REDUNDANT_NULLABLE")
    class object {
        val foo: String?? = null <!USELESS_CAST!>as<!> Nothing??
    }
}