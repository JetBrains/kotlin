// PROBLEM: none
fun foo() {
    val s1: String? = bar()
    val s2: String?<caret> = null
}

fun bar(): String? = ""
