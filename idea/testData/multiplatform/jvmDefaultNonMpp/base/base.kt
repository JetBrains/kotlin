package base

interface Check {
    fun test(): String {
        return "fail";
    }
}

open class CheckClass : Check