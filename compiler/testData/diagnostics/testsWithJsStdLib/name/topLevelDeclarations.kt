// FIR_IDENTICAL

// FILE: DeclarationOverloads.kt
package DeclarationOverloads

<!JS_NAME_CLASH!>fun test()<!> {}
fun test(x: Int) = x
fun test(x: String) = x
fun test(x: String?) = x
fun test(vararg x: Any) = x

@Suppress("NOTHING_TO_INLINE")
inline fun <reified T> test() {}

<!JS_NAME_CLASH!>val test<!> = 0

fun Int.test() {}
fun Int.test(x: Int)  = x

fun String.test() {}
fun String.test(x: String)  = x

val Int.test
    get() = 0

var String.test
    get() = ""
    set(value) { test(value) }


// FILE: ClashJsNamedFunctionWithOtherFunction.kt
package ClashJsNamedFunctionWithOtherFunction

<!JS_NAME_CLASH!>@JsName("test") fun noTest(x: String): String<!> = x

<!JS_NAME_CLASH!>fun test()<!> {}

// FILE: ClashJsNamedFunctionWithOtherProperty.kt
package ClashJsNamedFunctionWithOtherProperty

<!JS_NAME_CLASH!>@JsName("test") fun noTest(x: String): String<!> = x

<!JS_NAME_CLASH!>val test<!> = 1

// FILE: ClashJsNamedPropertyWithOtherFunction.kt
package ClashJsNamedPropertyWithOtherFunction

<!JS_NAME_CLASH!>fun test()<!> {}

<!JS_NAME_CLASH!>@JsName("test") val notTest<!> = 1

// FILE: ClashJsNamedPropertyGetterWithOtherFunction.kt
package ClashJsNamedPropertyGetterWithOtherFunction

<!JS_NAME_CLASH!>fun test()<!> {}

val notTest: Int
    <!JS_NAME_CLASH!>@JsName("test") get()<!> = 1

// FILE: ClashJsNamedPropertySetterWithOtherFunction.kt
package ClashJsNamedPropertySetterWithOtherFunction

<!JS_NAME_CLASH!>fun test()<!> {}

fun <T> ignore(x: T) = x

var notTest: Int
    @JsName("getterTest") get() = 1
    <!JS_NAME_CLASH!>@JsName("test") set(value)<!> { ignore(value) }

// FILE: FunctionAndInterfaceClash.kt
package FunctionAndInterfaceClash

<!JS_NAME_CLASH!>fun test()<!> {}

interface <!JS_NAME_CLASH!>test<!>

// FILE: FunctionWithParamsAndInterfaceNoClash.kt
package FunctionWithParamsAndInterfaceNoClash

fun test(x: Int) = x
@Suppress("NOTHING_TO_INLINE")
inline fun <reified T> test() {}

interface test

// FILE: FunctionWithJsNameAndInterfaceClash.kt
package FunctionWithJsNameAndInterfaceClash

<!JS_NAME_CLASH!>@JsName("test") fun notTest(x: Int)<!> = x

interface <!JS_NAME_CLASH!>test<!>

// FILE: FunctionWithJsNameAndObjectClash.kt
package FunctionWithJsNameAndObjectClash

<!JS_NAME_CLASH!>@JsName("test") fun notTest(x: Int)<!> = x

<!JS_NAME_CLASH!>object test<!>

// FILE: FunctionWithJsNameAndClassClash.kt
package FunctionWithJsNameAndClassClash

<!JS_NAME_CLASH!>@JsName("test") fun notTest(x: Int)<!> = x

class <!JS_NAME_CLASH!>test<!>

// FILE: FunctionAndInterfaceWithJsNameClash.kt
package FunctionAndInterfaceWithJsNameClash

<!JS_NAME_CLASH!>fun test()<!> {}

@JsName("test") interface <!JS_NAME_CLASH!>NotTest<!>

// FILE: FunctionAndClassWithJsNameClash.kt
package FunctionAndClassWithJsNameClash

<!JS_NAME_CLASH!>fun test()<!> {}

@JsName("test") class <!JS_NAME_CLASH!>NotTest<!>

// FILE: FunctionAndObjectWithJsNameClash.kt
package FunctionAndObjectWithJsNameClash

<!JS_NAME_CLASH!>fun test()<!> {}

@JsName("test") <!JS_NAME_CLASH!>object NotTest<!>

// FILE: PropertyWithJsNameAndInterfaceClash.kt
package PropertyWithJsNameAndInterfaceClash

<!JS_NAME_CLASH!>@JsName("test") val notTest<!> = 1

interface <!JS_NAME_CLASH!>test<!>

// FILE: FunctionWithJsNameAndExternalDeclarationsNoClash.kt
package FunctionWithJsNameAndExternalDeclarationsNoClash

@JsName("test")
fun notTest1(x: Int) = x

external fun test(): Int
external fun test(x: String): Int
external val test: Int

@JsName("test")
external fun NotTest2(x: Int): Int

@JsName("test")
external val NotTest3: Int

@JsName("test")
external interface NotTest4

@JsName("test")
external class NotTest5

@JsName("test")
external abstract class NotTest6

@JsName("test")
external open class NotTest7

@JsName("test")
external private class NotTest8

@JsName("test")
external internal class NotTest9

@JsName("test")
external object NotTest10
