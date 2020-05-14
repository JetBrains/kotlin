var result = ""

fun overload() {
    fun local() {
        result += "1"
    }
    local()
}

fun overload(unused: String) {
    fun local() {
        result += "2"
    }
    local()
    if ("".length < 1) {
        fun local() {
            result += "3"
        }
        local()
    }
}

class C {
    fun overload() {
        fun local() {
            result += "4"
        }
        local()
    }

    fun overload(unused: String) {
        fun local() {
            result += "5"
        }
        local()
        if ("".length < 1) {
            fun local() {
                result += "6"
            }
            local()
        }
    }
}

fun box(): String {
    overload()
    overload("")
    C().overload()
    C().overload("")
    return if (result == "123456") "OK" else "Fail: $result"
}
