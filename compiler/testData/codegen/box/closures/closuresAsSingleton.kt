// KT-64803

// INDY lambdas are not singletons on Android
// IGNORE_BACKEND: ANDROID

fun generateClosure(): () -> Unit = {}

fun box(): String {
    return if (generateClosure().hashCode() != generateClosure().hashCode()) "Fail: hashCode are not equals" else "OK"
}
