// TARGET_BACKEND: WASM

// FILE: reflectionOnExternals.js
class Outer { }

Outer.Inner = Promise;

// FILE: reflectionOnExternals.kt
import kotlin.js.Promise
import kotlin.reflect.KClass
import kotlin.reflect.typeOf

@JsName("Promise")
external class PromiseAlias : JsAny

external class Outer : JsAny {
    class Inner : JsAny
    @JsName("Inner")
    class InnerAlias : JsAny
}

fun checkPromise(kClass: KClass<*>, promiseObject: Any, nonPromiseObject: Any): String? {
    if (kClass.simpleName != "Promise") return "FAIL1"
    if (kClass != Promise::class) return "FAIL2"
    if (Promise::class != kClass) return "FAIL3"
    if (!kClass.isInstance(promiseObject)) return "FAIL4"
    if (kClass.isInstance(nonPromiseObject)) return "FAIL5"
    return null
}

fun box(): String {
    val promiseObj: Any = Promise<JsAny> { resolve, reject -> Unit }
    val someObject: Any = Any()
    checkPromise(Promise::class, promiseObj, someObject)?.let { return "1_" + it }
    checkPromise(PromiseAlias::class, promiseObj, someObject)?.let { return "2_" + it }
    checkPromise(promiseObj::class, promiseObj, someObject)?.let { return "3_" + it }
    checkPromise(Outer.Inner::class, promiseObj, someObject)?.let { return "4_" + it }
    checkPromise(Outer.InnerAlias::class, promiseObj, someObject)?.let { return "5_" + it }

    val typeOfPromiseClassifier = typeOf<Promise<*>>().classifier
    if (typeOfPromiseClassifier !is KClass<*>) return "FAIL6"
    checkPromise(typeOfPromiseClassifier, promiseObj, someObject)?.let { return "7_" + it }

    val typeOfPromiseWithArguments = typeOf<Promise<Promise<*>>>()
    if (typeOfPromiseWithArguments.arguments.size != 1) return "FAIL8"
    val typeOfPromiseArgumentClassifier = typeOfPromiseWithArguments.arguments[0].type?.classifier
    if (typeOfPromiseArgumentClassifier !is KClass<*>) return "FAIL9"
    checkPromise(typeOfPromiseArgumentClassifier, promiseObj, someObject)?.let { return "10_" + it }

    return "OK"
}
