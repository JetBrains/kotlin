// "Import" "false"
// ERROR: Unresolved reference. None of the following candidates is applicable because of receiver type mismatch: <br>public operator fun String?.plus(other: Any?): String defined in kotlin
// ACTION: Create extension function 'JustClass.plus'
// ACTION: Create member function 'JustClass.plus'
// ACTION: Flip '+'
// ACTION: Replace overloaded operator with function call
// COMPILER_ARGUMENTS: -XXLanguage:-NewInference


class JustClass
fun some() {
    val x = JustClass() +<caret> 1
}