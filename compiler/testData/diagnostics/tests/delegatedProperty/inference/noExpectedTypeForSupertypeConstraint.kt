// !WITH_NEW_INFERENCE
import kotlin.reflect.KProperty

class A {
    var a by <!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>MyProperty<!>()
}

class MyProperty<T, R> {

    operator fun getValue(thisRef: R, desc: KProperty<*>): T {
        throw Exception("$thisRef $desc")
    }

    operator fun setValue(thisRef: R, desc: KProperty<*>, t: T) {
        throw Exception("$thisRef $desc $t")
    }
}
