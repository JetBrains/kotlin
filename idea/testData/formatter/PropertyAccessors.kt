class Hi {
    val simple: Int          get() = 1

    val newline: String



               get() = ""

    var getterAndSetter: Int = 0

get() = 1


    set(some) {
            field = some
        }


    var badNoType get() = 1
}

class EmptyProperties {
    var newline: String
        get() { return "" }
        set(value) {}
}

class EmptyProperties {
    /**
     *
     */
    var newline: String
        /**
         *
         */
        get() { return "" }

        /**
         *
         */
        set(value) {}
}