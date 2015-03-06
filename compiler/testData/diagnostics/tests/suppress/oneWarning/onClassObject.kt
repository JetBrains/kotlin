class C {
    suppress("REDUNDANT_NULLABLE")
    default object {
        val foo: String?? = null <!USELESS_CAST!>as Nothing??<!>
    }
}