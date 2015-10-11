class A {
    var a by <!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>MyProperty<!>()
}

class MyProperty<T, R> {

    operator fun getValue(thisRef: R, desc: PropertyMetadata): T {
        throw Exception("$thisRef $desc")
    }

    operator fun setValue(thisRef: R, desc: PropertyMetadata, t: T) {
        throw Exception("$thisRef $desc $t")
    }
}
