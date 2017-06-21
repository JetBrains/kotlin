// PROBLEM: none
var a: Testtt? = Testtt()

fun main(args: Array<String>) {
    if (a<caret> != null && a?.a != null) {
 
    }
}

class Testtt {
    val a: Testtt? = null
}