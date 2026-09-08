// WITH_REFLECT
// FULL_JDK
// TARGET_BACKEND: JVM
// java.beans is not available on Android
// IGNORE_BACKEND: ANDROID

// KT-71061 (duplicate of KT-52706): the mangled accessors of a property whose type is a *generic* inline
// class used to be emitted with the inline class' own type parameter left unsubstituted in their generic
// JVM signature (`()Ljava/util/List<TT;>;` instead of `()Ljava/util/List<Ljava/lang/String;>;`).
// `T` is not in scope in the containing class, so java.beans.TypeResolver failed to resolve it and NPE'd.

import java.beans.Introspector
import java.beans.PropertyDescriptor
import kotlin.reflect.jvm.javaMethod

@JvmInline
value class ValueClass<T>(val a: List<T>)

class NonValueClass<T>(val a: List<T>)

class PropContainer(var a: ValueClass<String>, var b: NonValueClass<Boolean>)

fun box(): String {
    // Control: the non-value-class property always worked.
    PropertyDescriptor(
        "b", PropContainer::class.java,
        PropContainer::b.getter.javaMethod!!.name,
        PropContainer::b.setter.javaMethod!!.name,
    )

    // The regression: mangled accessors must carry a resolvable generic signature.
    val mangled = PropertyDescriptor(
        "a", PropContainer::class.java,
        PropContainer::a.getter.javaMethod!!.name,
        PropContainer::a.setter.javaMethod!!.name,
    )
    if (mangled.propertyType != List::class.java) return "FAIL mangled property type: ${mangled.propertyType}"

    // A full getBeanInfo() walk resolves the signature of *every* method; this is what NPE'd in the report.
    Introspector.getBeanInfo(PropContainer::class.java)

    return "OK"
}
