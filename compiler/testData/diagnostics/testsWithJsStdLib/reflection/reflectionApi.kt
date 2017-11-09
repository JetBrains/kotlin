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
    k.<!UNSUPPORTED!>members<!>
    k.<!UNSUPPORTED!>constructors<!>
    k.<!UNSUPPORTED!>nestedClasses<!>
    k.<!UNSUPPORTED!>objectInstance<!>
    k.<!UNSUPPORTED!>typeParameters<!>
    k.<!UNSUPPORTED!>supertypes<!>
    k.<!UNSUPPORTED!>visibility<!>
    k.<!UNSUPPORTED!>isFinal<!>
    k.<!UNSUPPORTED!>isOpen<!>
    k.<!UNSUPPORTED!>isAbstract<!>
    k.<!UNSUPPORTED!>isSealed<!>
    k.<!UNSUPPORTED!>isData<!>
    k.<!UNSUPPORTED!>isInner<!>
    k.<!UNSUPPORTED!>isCompanion<!>

    k.<!UNSUPPORTED!>annotations<!>

    k == kt
    k.hashCode()
    k.toString()
}
