// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
import kotlin.reflect.KProperty

class MyMetadata<in T, R>(val default: R) {
    operator fun getValue(thisRef: T, desc: KProperty<*>): R = TODO()
    operator fun setValue(thisRef: T, desc: KProperty<*>, value: R) {}
}

interface Something
class MyReceiver
var MyReceiver.something: Something? by MyMetadata(default = null)

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, in, interfaceDeclaration, nullableType, operator,
primaryConstructor, propertyDeclaration, propertyDelegate, propertyWithExtensionReceiver, setter, starProjection,
typeParameter */
