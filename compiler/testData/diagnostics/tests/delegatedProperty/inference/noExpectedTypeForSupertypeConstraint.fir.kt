// RUN_PIPELINE_TILL: FRONTEND
// NI_EXPECTED_FILE
import kotlin.reflect.KProperty

class A {
    var a <!DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE!>by<!> <!CANNOT_INFER_PARAMETER_TYPE, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>MyProperty<!>()
}

class MyProperty<T, R> {

    operator fun getValue(thisRef: R, desc: KProperty<*>): T {
        throw Exception("$thisRef $desc")
    }

    operator fun setValue(thisRef: R, desc: KProperty<*>, t: T) {
        throw Exception("$thisRef $desc $t")
    }
}
