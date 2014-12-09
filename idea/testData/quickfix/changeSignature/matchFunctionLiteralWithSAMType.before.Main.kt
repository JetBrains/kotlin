// "Change the signature of function literal" "true"
// DISABLE-ERRORS

fun main(args: Array<String>) {
    Test<String>().perform("") <caret>{  }
}
