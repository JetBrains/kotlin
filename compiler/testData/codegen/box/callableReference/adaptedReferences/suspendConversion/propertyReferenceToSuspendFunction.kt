// ISSUE: KT-73399

fun interface Some {
    fun myInvoke(): Any?
}

fun interface SomeSuspend {
    suspend fun myInvoke(): Any?
}

fun executeSuspend(f: suspend () -> Any?): String = "1"
fun executeSam(f: Some): String = "2"
fun executeSamSuspend(f: SomeSuspend): String = "3"

fun test(propertyReference: kotlin.reflect.KProperty0<Any?>): String {
    val one = executeSuspend(propertyReference)
    val two = executeSam(propertyReference)
    val three = executeSamSuspend(propertyReference)
    return one + two + three
}

val x: Int = 1
fun box(): String {
    val result =  test(::x)
    return if (result == "123") {
        "OK"
    } else {
        "Fail: $result"
    }
}
