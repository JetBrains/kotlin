import kotlin.reflect.KClass

fun <T : Any> foo(kClass: KClass<T>){}
fun <T : Any> foo(kClass: KClass<T>?, p: Int){}

fun f() {
    foo<Int>(<caret>)
}

// WITH_ORDER
// EXIST: { lookupString: "Int::class", itemText: "Int::class", attributes: "" }
// EXIST: { lookupString: "object" }
// EXIST: null
// NOTHING_ELSE
