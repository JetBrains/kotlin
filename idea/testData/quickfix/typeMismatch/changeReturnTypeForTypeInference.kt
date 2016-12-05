// "Change return type of enclosing function 'test2' to 'List<Any>'" "true"
// WITH_RUNTIME

fun test2(ss: List<Any>) {
    return ss.map { it }<caret>
}