// IS_APPLICABLE: false
fun get(x: Int, y: Int) : Boolean {
    return false
}

fun bar(){
    get<caret>(1,2)
}