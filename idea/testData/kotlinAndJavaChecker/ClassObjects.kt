class WithClassObject {
    default object {
        fun foo() {}

        val value: Int = 0
        val valueWithGetter: Int
             get() = 1

        var variable: Int = 0
        var variableWithAccessors: Int
            get() = 0
            set(v) {}

    }
}