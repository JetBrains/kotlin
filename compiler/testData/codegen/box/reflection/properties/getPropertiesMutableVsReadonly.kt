// WITH_REFLECT

import kotlin.reflect.*

class A(val readonly: String) {
    var mutable: String = "before"
}

fun box(): String {
    val props = A::class.java.kotlin.memberProperties
    val readonly = props.single { it.name == "readonly" }
    assert(readonly !is KMutableProperty1<A, *>) { "Fail 1: $readonly" }
    val mutable = props.single { it.name == "mutable" }
    assert(mutable is KMutableProperty1<A, *>) { "Fail 2: $mutable" }

    val a = A("")
    mutable as KMutableProperty1<A, String>
    assert(mutable.get(a) == "before") { "Fail 3: ${mutable.get(a)}" }
    mutable.set(a, "OK")
    return mutable.get(a)
}
