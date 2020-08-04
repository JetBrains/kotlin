object FunnyObject {
    fun makeFun() {}
}

fun test() {
    <!FUNNY_NAME_NOT_ALLOWED!>FunnyObject<!>

    FunnyObject.makeFun()
}