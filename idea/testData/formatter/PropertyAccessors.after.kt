class Hi {
    val simple: Int get() = 1

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
        get() {
            return ""
        }
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
        get() {
            return ""
        }
        /**
         *
         */
        set(value) {}
}

var prop: Int // Int
    get() = 1 // this is getter
    set(value) {} // this is setter

val prop2: Int get = 1 // prop2

var prop3: Int // Int
    // this comment is for getter
    get() = 1
    // this comment is for setter
    set(value) {}

val prop4: Int
    get() = 42
    set(value) {}