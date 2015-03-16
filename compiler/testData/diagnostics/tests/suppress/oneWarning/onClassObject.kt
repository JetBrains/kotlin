class C {
    suppress("REDUNDANT_NULLABLE")
    companion object {
        val foo: String?? = null <!USELESS_CAST!>as Nothing??<!>
    }
}