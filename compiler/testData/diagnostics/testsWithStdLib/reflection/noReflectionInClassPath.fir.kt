import kotlin.reflect.*

class Foo(val prop: Any) {
    fun func() {}
}

fun n01() = Foo::prop
fun n02() = Foo::func
fun n03() = Foo::class
fun n04(p: KProperty0<Int>) = p.get()
fun n05(p: KMutableProperty0<String>) = p.set("")
fun n07(p: KFunction<String>) = p.name
fun n08(p: KProperty1<String, Int>) = p.get("")
fun n09(p: KProperty2<String, String, Int>) = p.get("", "")
fun n10() = (Foo::func).invoke(Foo(""))
fun n11() = (Foo::func)(Foo(""))

fun y01() = Foo::prop.getter
fun y02() = Foo::class.members
fun y03() = Foo::class.simpleName
fun y04() = Foo::class.<!UNRESOLVED_REFERENCE!>properties<!>

fun <T : Any> kclass(k: KClass<*>, kt: KClass<T>) {
    k.simpleName
    k.qualifiedName
    k.members
    k.constructors
    k.nestedClasses
    k.objectInstance
    k.typeParameters
    k.supertypes
    k.visibility
    k.isFinal
    k.isOpen
    k.isAbstract
    k.isSealed
    k.isData
    k.isInner
    k.isCompanion

    k.annotations
    k.isInstance(42)

    k == kt
    k.hashCode()
    k.toString()
}

fun ktype(t: KType, t2: KType) {
    t.classifier
    t.arguments
    t.isMarkedNullable
    t.annotations

    t == t2
    t.hashCode()
    t.toString()

    KTypeProjection.Companion.covariant(t)
    KTypeProjection.STAR
    KTypeProjection(KVariance.IN, t)
}
