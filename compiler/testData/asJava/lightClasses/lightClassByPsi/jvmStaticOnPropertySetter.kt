class MyClass {
    companion object {
        var prop: Int = 4
            @JvmStatic
            set(value) {
                field = value
            }

            get() = field

        @get:JvmStatic
        var prop2: String = ""
            set(value) {
                field = value
            }
    }
}
