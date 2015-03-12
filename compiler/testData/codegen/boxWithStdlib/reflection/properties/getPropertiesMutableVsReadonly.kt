import kotlin.reflect.jvm.kotlin
import kotlin.reflect.KMutableMemberProperty

class A(val readonly: String) {
    var mutable: String = "before"
}

fun box(): String {
    val props = javaClass<A>().kotlin.getProperties()
    val readonly = props.single { it.name == "readonly" }
    assert(readonly !is KMutableMemberProperty<A, *>) { "Fail 1: $readonly" }
    val mutable = props.single { it.name == "mutable" }
    assert(mutable is KMutableMemberProperty<A, *>) { "Fail 2: $mutable" }

    val a = A("")
    mutable as KMutableMemberProperty<A, String>
    assert(mutable[a] == "before") { "Fail 3: ${mutable.get(a)}" }
    mutable[a] = "OK"
    return mutable.get(a)
}
