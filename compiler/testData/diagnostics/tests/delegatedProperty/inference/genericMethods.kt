// !DIAGNOSTICS: -UNUSED_PARAMETER
// !WITH_NEW_INFERENCE
// NI_EXPECTED_FILE
import kotlin.reflect.KProperty

//var a: Int by A()
//
//class A {
//  operator fun <T> getValue(t: Any?, p: KProperty<*>): T = null!!
//  operator fun <T> setValue(t: Any?, p: KProperty<*>, x: T) = Unit
//}

class MyMetadata<R>(val default: R) {
  operator fun getValue(thisRef: Any?, desc: KProperty<*>): R = TODO()
  operator fun setValue(thisRef: Any?, desc: KProperty<*>, value: R) {}
}

interface Something
var something: Something? by MyMetadata(default = null)