// FIR_IDENTICAL
// In K2, the name collision detector is weakened, because the backend started to resolve such collisions.
// K1 was not changed since it's in maintenance mode.

// FILE: DeclarationOverloads.kt
package DeclarationOverloads

fun test() {}
fun test(x: Int) = x
fun test(x: String) = x
fun test(x: String?) = x
fun test(vararg x: Any) = x

@Suppress("NOTHING_TO_INLINE")
inline fun <reified T> test() {}

val test = 0

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

@JsName("test") fun noTest(x: String): String = x

fun test() {}

// FILE: ClashJsNamedFunctionWithOtherProperty.kt
package ClashJsNamedFunctionWithOtherProperty

@JsName("test") fun noTest(x: String): String = x

val test = 1

// FILE: ClashJsNamedPropertyWithOtherFunction.kt
package ClashJsNamedPropertyWithOtherFunction

fun test() {}

@JsName("test") val notTest = 1

// FILE: ClashJsNamedPropertyGetterWithOtherFunction.kt
package ClashJsNamedPropertyGetterWithOtherFunction

fun test() {}

val notTest: Int
    @JsName("test") get() = 1

// FILE: ClashJsNamedPropertySetterWithOtherFunction.kt
package ClashJsNamedPropertySetterWithOtherFunction

fun test() {}

fun <T> ignore(x: T) = x

var notTest: Int
    @JsName("getterTest") get() = 1
    @JsName("test") set(value) { ignore(value) }

// FILE: FunctionAndInterfaceClash.kt
package FunctionAndInterfaceClash

fun test() {}

interface test

// FILE: FunctionWithParamsAndInterfaceNoClash.kt
package FunctionWithParamsAndInterfaceNoClash

fun test(x: Int) = x
@Suppress("NOTHING_TO_INLINE")
inline fun <reified T> test() {}

interface test

// FILE: FunctionWithJsNameAndInterfaceClash.kt
package FunctionWithJsNameAndInterfaceClash

@JsName("test") fun notTest(x: Int) = x

interface test

// FILE: FunctionWithJsNameAndObjectClash.kt
package FunctionWithJsNameAndObjectClash

@JsName("test") fun notTest(x: Int) = x

object test

// FILE: FunctionWithJsNameAndClassClash.kt
package FunctionWithJsNameAndClassClash

@JsName("test") fun notTest(x: Int) = x

class test

// FILE: FunctionAndInterfaceWithJsNameClash.kt
package FunctionAndInterfaceWithJsNameClash

fun test() {}

@JsName("test") interface NotTest

// FILE: FunctionAndClassWithJsNameClash.kt
package FunctionAndClassWithJsNameClash

fun test() {}

@JsName("test") class NotTest

// FILE: FunctionAndObjectWithJsNameClash.kt
package FunctionAndObjectWithJsNameClash

fun test() {}

@JsName("test") object NotTest

// FILE: PropertyWithJsNameAndInterfaceClash.kt
package PropertyWithJsNameAndInterfaceClash

@JsName("test") val notTest = 1

interface test

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
