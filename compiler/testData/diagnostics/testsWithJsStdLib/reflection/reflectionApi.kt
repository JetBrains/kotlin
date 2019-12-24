// !DIAGNOSTICS: -UNUSED_EXPRESSION
import kotlin.reflect.*

class Foo(val prop: Any) {
    fun func() {}
}

fun testSomeValidCases(p0: KProperty0<Int>, pm0: KMutableProperty0<String>, f: KFunction<String>, p1: KProperty1<String, Int>, p2: KProperty2<String, String, Int>) {
    Foo::prop
    Foo::func
    Foo::class
    p0.get()
    p0.name
    pm0.set("")
    f.name
    p1.get("")
    p2.get("", "")
    (Foo::func).invoke(Foo(""))
    (Foo::func)(Foo(""))

    p0 == pm0
    p1.equals(p2)
    p0.hashCode()
    f.toString()
}

fun <T : Any> kclass(k: KClass<*>, kt: KClass<T>) {
    k.simpleName
    k.<!UNSUPPORTED!>qualifiedName<!>
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

    k == kt
    k.hashCode()
    k.toString()
}
