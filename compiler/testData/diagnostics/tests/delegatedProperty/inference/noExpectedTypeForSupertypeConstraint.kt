// !WITH_NEW_INFERENCE
// NI_EXPECTED_FILE
import kotlin.reflect.KProperty

class A {
    var a by <!NI;DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE!><!NI;NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER, OI;TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>MyProperty<!>()<!>
}

class MyProperty<T, R> {

    operator fun getValue(thisRef: R, desc: KProperty<*>): T {
        throw Exception("$thisRef $desc")
    }

    operator fun setValue(thisRef: R, desc: KProperty<*>, t: T) {
        throw Exception("$thisRef $desc $t")
    }
}
