// !DIAGNOSTICS: -UNUSED_PARAMETER
// ISSUE: KT-61077
// IGNORE_BACKEND_K1: ANY
// WITH_STDLIB

data class Delegate<T>(val data: T)

var counter = 0

operator fun Delegate<*>.getValue(receiver: Any?, p: Any): String = this.data.toString()
operator fun <T> Delegate<T>.setValue(receiver: Any?, p: Any, value: T) {
    require(value == "OK")
    counter++
}

operator fun <T> String.provideDelegate(receiver: Any?, p: Any) = Delegate<T>(this as T)

var test1: String by Delegate("OK1")
var test2: String by Delegate<String>("OK2")

var test3: String by "OK3"

var test4: String by "OK4".provideDelegate(null, "")
var test5: String by "OK5".provideDelegate<String>(null, "")

fun box(): String {
    require(test1 == "OK1")
    test1 = "OK"
    require(test2 == "OK2")
    test2 = "OK"
    require(test3 == "OK3")
    test3 = "OK"
    require(test4 == "OK4")
    test4 = "OK"
    require(test5 == "OK5")
    test5 = "OK"
    require(counter == 5) { "Counter counter is $counter" }
    
    return "OK"
}
