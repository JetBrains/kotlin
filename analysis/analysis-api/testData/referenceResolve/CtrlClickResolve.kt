package ctrl_click

fun IntArray(vararg content : String) : Array<out String> = content
inline fun <reified T> array(vararg t : T) : Array<T> = arrayOf(*t)

fun main(args : Array<String>) {
    var a = <caret>IntArray(*array("1", "2", "3"))
}

