import kotlin.reflect.KClass

fun <T : Any> foo(kClass: KClass<T>){}
fun <T : Any> foo(kClass: KClass<T>?, p: Int){}

fun f() {
    foo<Int>(<caret>)
}

// WITH_ORDER
// EXIST: { lookupString: "Int", itemText: "Int::class", attributes: "" }
// EXIST: { lookupString: "object" }
// EXIST: null
// EXIST: { lookupString: "kClass = null", itemText: "kClass = null" }
// NOTHING_ELSE
