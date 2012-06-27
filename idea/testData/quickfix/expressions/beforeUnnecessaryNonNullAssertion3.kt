// "Remove unnecessary non-null assertion (!!)" "false"
fun test(value : String) : Int {
    return value<caret>!!.length()
}
