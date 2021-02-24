package base

interface Check {
    fun test(): String {
        return "fail";
    }

    var test: String
        get() = "123"
        set(value) { value.length}
}

open class CheckClass : Check