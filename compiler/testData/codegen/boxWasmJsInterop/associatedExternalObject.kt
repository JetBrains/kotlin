// TARGET_BACKEND: WASM

// FILE: findAssociatedExternalObject.js

const JS_OBJECT = {}

// FILE: findAssociatedExternalObject.kt

import kotlin.reflect.*

@OptIn(ExperimentalAssociatedObjects::class)
@AssociatedObjectKey
@Retention(AnnotationRetention.BINARY)
annotation class Associated(val kClass: KClass<*>)

external object JS_OBJECT

@Associated(JS_OBJECT::class)
class ObjectKey

@OptIn(ExperimentalAssociatedObjects::class)
fun box(): String {
    if (ObjectKey::class.findAssociatedObject<Associated>() !== JS_OBJECT) return "FAIL2"
    return "OK"
}