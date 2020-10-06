// FIR_COMPARISON
open class Base

class Test
fun <T> Test.extensionEmpty(param: T) = "Test"
fun <T: Base> Test.extensionBase(param: T) = "Test"
fun <T: Base, P> Test.extensionTwo(param: T) = "Test"

fun some() {
    Test().ex<caret>
}

// EXIST: extensionEmpty
// EXIST: extensionBase
// EXIST: extensionTwo
