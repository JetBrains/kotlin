// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: Bar.java
public interface Bar<T> {
}

// FILE: Foo.java
public interface Foo<T> extends Bar<T> {
}

// FILE: box.kt
import kotlin.reflect.typeOf
import kotlin.reflect.KType
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.isSupertypeOf
import kotlin.test.assertFalse

interface Baz

fun checkUnrelatedTypes(subtype: KType, supertype: KType) {
    assertFalse(subtype.isSubtypeOf(supertype), "Expected $subtype NOT to be a subtype of $supertype")
    assertFalse(supertype.isSubtypeOf(subtype), "Expected $supertype NOT to be a subtype of $subtype")
    assertFalse(subtype.isSupertypeOf(supertype), "Expected $supertype NOT to be a subtype of $subtype")
    assertFalse(supertype.isSupertypeOf(subtype), "Expected $subtype NOT to be a subtype of $supertype")
}

fun <T> checkNonReifiedDnnType() {
    checkUnrelatedTypes(typeOf<Foo<in T & Any>>(), typeOf<Baz>())
}

fun box(): String {
    checkUnrelatedTypes(typeOf<Foo<in String>>(), typeOf<Baz>())

    checkNonReifiedDnnType<Any>()

    return "OK"
}
