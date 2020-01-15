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

fun y01() = Foo::prop.<!UNRESOLVED_REFERENCE!>getter<!>
fun y02() = Foo::class.<!UNRESOLVED_REFERENCE!>members<!>
fun y03() = Foo::class.simpleName
fun y04() = Foo::class.<!UNRESOLVED_REFERENCE!>properties<!>

fun <T : Any> kclass(k: KClass<*>, kt: KClass<T>) {
    k.simpleName
    k.qualifiedName
    k.<!UNRESOLVED_REFERENCE!>members<!>
    k.<!UNRESOLVED_REFERENCE!>constructors<!>
    k.<!UNRESOLVED_REFERENCE!>nestedClasses<!>
    k.<!UNRESOLVED_REFERENCE!>objectInstance<!>
    k.<!UNRESOLVED_REFERENCE!>typeParameters<!>
    k.<!UNRESOLVED_REFERENCE!>supertypes<!>
    k.<!UNRESOLVED_REFERENCE!>visibility<!>
    k.<!UNRESOLVED_REFERENCE!>isFinal<!>
    k.<!UNRESOLVED_REFERENCE!>isOpen<!>
    k.<!UNRESOLVED_REFERENCE!>isAbstract<!>
    k.<!UNRESOLVED_REFERENCE!>isSealed<!>
    k.<!UNRESOLVED_REFERENCE!>isData<!>
    k.<!UNRESOLVED_REFERENCE!>isInner<!>
    k.<!UNRESOLVED_REFERENCE!>isCompanion<!>

    k.<!UNRESOLVED_REFERENCE!>annotations<!>
    k.isInstance(42)

    k == kt
    k.hashCode()
    k.toString()
}

fun ktype(t: KType, t2: KType) {
    t.classifier
    t.arguments
    t.isMarkedNullable
    t.<!UNRESOLVED_REFERENCE!>annotations<!>

    t == t2
    t.hashCode()
    t.toString()

    KTypeProjection.Companion.covariant(t)
    KTypeProjection.STAR
    KTypeProjection(KVariance.IN, t)
}
