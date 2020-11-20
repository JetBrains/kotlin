// PROBLEM: none
fun elvisFun(str: String?): String?<caret> {
    val v = str?.length ?: return null
    return v.toString()
}