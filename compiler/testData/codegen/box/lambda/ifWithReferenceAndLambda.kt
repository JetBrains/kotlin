// IGNORE_BACKEND: ANDROID
// WITH_STDLIB
// ISSUE: KT-63258

class MethodReferenceNPE() {
    private val singleRef: () -> Unit =
        1.let {
            if (it == 0) {
                { }
            } else {
                this@MethodReferenceNPE::myMethod
            }
        }

    private fun myMethod() {}

    fun run() {
        singleRef()
    }
}

fun box(): String {
    MethodReferenceNPE().run()
    return "OK"
}
