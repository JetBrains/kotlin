// KT-3343 Type mismatch when function literal consists of try-catch with Int returning call, and Unit is expected

fun main() {
    "hello world".prt{
        try{
            print(it)
            log("we are printing")
        }
        catch(e : Exception){
            log("Exception")
        }
    }
}

fun log(str : String) : Int{
    print("logging $str")
    return 0
}

fun print(<!UNUSED_PARAMETER!>obj<!>: Any) {}


fun String.prt(action : (String) -> Unit){
    action(this)
}