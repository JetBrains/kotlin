// COMPILER_ARGUMENTS: -XXLanguage:+NewInference
// IS_APPLICABLE: false

interface List<T>

class ArrayList<T> : List<T>

fun test() {
    val list: List<*> = ArrayList<caret><Any?>()
}