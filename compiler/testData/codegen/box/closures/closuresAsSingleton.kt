// KT-64803

fun generateClosure(): () -> Unit = {}

fun box(): String {
    return if (generateClosure().hashCode() != generateClosure().hashCode()) "Fail: hashCode are not equals" else "OK"
}
