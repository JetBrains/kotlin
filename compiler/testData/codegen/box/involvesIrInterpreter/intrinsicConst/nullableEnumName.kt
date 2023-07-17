// TARGET_BACKEND: JVM_IR
// TARGET_BACKEND: JS_IR
// TARGET_BACKEND: NATIVE
// WITH_STDLIB
fun <T> T.id() = this

enum class EnumClass {
    OK
}

val shouldNotBeEvaluated1 = EnumClass.OK?.name ?: ""
val shouldNotBeEvaluated2 = EnumClass.OK?.name?.toString()
val shouldNotBeEvaluated3 = EnumClass.OK?.name.toString()

fun box(): String {
    if (shouldNotBeEvaluated1 != "OK") return "Fail 1"
    if (shouldNotBeEvaluated2 != "OK") return "Fail 2"
    if (shouldNotBeEvaluated3 != "OK") return "Fail 3"
    return shouldNotBeEvaluated1.id()
}
