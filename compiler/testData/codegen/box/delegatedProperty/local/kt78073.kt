// ISSUE: KT-78073

import kotlin.reflect.KProperty

fun interface ReadOnlyProperty<in T, out V> {
    operator fun getValue(thisRef: T, property: KProperty<*>): V
}

fun box(): String {
    val property1 by ReadOnlyProperty { _, property -> property }
    val property2 by ReadOnlyProperty { _, property -> property }

    val property1Ref = property1
    if (property1Ref.name != "property1") return "Fail 1: ${property1Ref.name}"

    val property2Ref = property2 // invokes the property2 delegate
    if (property1Ref.name != "property1") return "Fail 2: ${property1Ref.name}"
    if (property2Ref.name != "property2") return "Fail 3: ${property2Ref.name}"

    property1 // invokes the property1 delegate
    if (property1Ref.name != "property1") return "Fail 4: ${property1Ref.name}"
    if (property2Ref.name != "property2") return "Fail 5: ${property2Ref.name}"

    return "OK"
}
