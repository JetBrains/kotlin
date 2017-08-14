import kotlin.reflect.KProperty

class MyMetadata<in T, R>(val default: R) {
    operator fun getValue(thisRef: T, desc: KProperty<*>): R = TODO()
    operator fun setValue(thisRef: T, desc: KProperty<*>, value: R) {}
}

interface Something
class MyReceiver
var MyReceiver.something: Something? by MyMetadata(default = null)
