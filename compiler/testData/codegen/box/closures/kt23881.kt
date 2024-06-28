// TARGET_BACKEND: JVM
// IGNORE_BACKEND: JVM
// LAMBDAS: CLASS
// WITH_STDLIB

class ShouldBeCaptured
class ShouldNOTBeCaptured

class ClassWithCallback {
    var someCallback: (() -> Unit)? = null

    fun checkFields(): String {
        for (field in someCallback!!.javaClass.declaredFields) {
            val value = field.get(someCallback!!)
            if (value is ShouldNOTBeCaptured) throw AssertionError("Leaked value")
        }
        return "OK"
    }
}

fun box(): String {
    val toCapture = ShouldBeCaptured()
    val notToCapture = ShouldNOTBeCaptured()

    val classWithCallback = ClassWithCallback()
    classWithCallback.apply {
        someCallback = { toCapture }
        notToCapture
    }
    return classWithCallback.checkFields()
}
