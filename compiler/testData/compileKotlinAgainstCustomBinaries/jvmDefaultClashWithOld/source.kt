import base.*

interface SubCheck : Check {
    override fun test(): String {
        return "OK"
    }

    override var test: String
        get() = "OK"
        set(value) {
            value.length
        }
}

class SubCheckClass : CheckClass(), SubCheck