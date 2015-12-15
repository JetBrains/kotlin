// "Change the signature of lambda expression" "true"
// DISABLE-ERRORS

fun main(args: Array<String>) {
    Test<String>().perform("") <caret>{  }
}
