// See KT-12337 Reference to property with invisible setter should not be a KMutableProperty
// JVM_ABI_K1_K2_DIFF: KT-63984

import kotlin.reflect.KProperty1
import kotlin.reflect.KMutableProperty

open class Bar(name: String) {
    var foo: String = name
        private set
}

class Baz : Bar("") {
    fun ref() = Bar::foo
}

fun box(): String {
    val p1: KProperty1<Bar, String> = Bar::foo
    if (p1 is KMutableProperty<*>) return "Fail: p1 is a KMutableProperty"

    val p2 = Baz().ref()
    if (p2 is KMutableProperty<*>) return "Fail: p2 is a KMutableProperty"

    val p3 = Bar("")::foo
    if (p3 is KMutableProperty<*>) return "Fail: p3 is a KMutableProperty"

    return p1.get(Bar("OK"))
}
