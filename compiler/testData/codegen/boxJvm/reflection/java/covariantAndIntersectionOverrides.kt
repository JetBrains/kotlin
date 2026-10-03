// TARGET_BACKEND: JVM
// WITH_REFLECT

// FILE: test/Getter.java
package test;

public interface Getter {
    Object get();
}

// FILE: test/StringGetter.java
package test;

public interface StringGetter {
    String get();
}

// FILE: test/Source.java
package test;

public interface Source<T> {
    T get();
}

// FILE: test/Both.java
package test;

public interface Both extends Getter, StringGetter {}

// FILE: test/Mixed.java
package test;

public interface Mixed extends Source<String>, Getter {}

// FILE: test/RawMixed.java
package test;

public interface RawMixed extends Source, StringGetter {}

// FILE: test/JImpl.java
package test;

public class JImpl implements Both {
    @Override
    public String get() { return "JImpl"; }
}

// FILE: test/JSub.java
package test;

// Inherits `get` from a Kotlin class which in turn implements a generic Kotlin interface, see KT-86926.
public class JSub extends KBase<CharSequence> {}

// FILE: test/box.kt
package test

import kotlin.reflect.*
import kotlin.test.*

// Intersection overrides of methods with covariant return types (as well as raw + generic ones, see KT-89732) must be loaded
// as a single member with the most specific return type, regardless of whether they are declared or inherited in Java or Kotlin.

abstract class KBoth : Both

abstract class KMixed : Mixed

abstract class KRawMixed : RawMixed

open class KBase<T : Any> : Source<T> {
    override fun get(): T = "KBase" as T
}

class KImpl : JImpl()

private fun KClass<*>.get(): KFunction<*> = members.single { it.name == "get" } as KFunction<*>

private fun check(expected: String, klass: KClass<*>, isAbstract: Boolean) {
    val get = klass.get()
    assertEquals(expected, get.toString())
    assertEquals(isAbstract, get.isAbstract, "$klass")
}

fun box(): String {
    check("fun test.Both.get(): kotlin.String!", Both::class, isAbstract = true)
    check("fun test.KBoth.get(): kotlin.String!", KBoth::class, isAbstract = true)
    check("fun test.Mixed.get(): kotlin.String!", Mixed::class, isAbstract = true)
    check("fun test.KMixed.get(): kotlin.String!", KMixed::class, isAbstract = true)
    check("fun test.RawMixed.get(): kotlin.String!", RawMixed::class, isAbstract = true)
    check("fun test.KRawMixed.get(): kotlin.String!", KRawMixed::class, isAbstract = true)

    check("fun test.JImpl.get(): kotlin.String!", JImpl::class, isAbstract = false)
    check("fun test.KImpl.get(): kotlin.String!", KImpl::class, isAbstract = false)
    assertEquals("JImpl", KImpl::class.get().call(KImpl()))
    // A member of a supertype can be called on a subclass instance, and the most specific override is invoked.
    assertEquals("JImpl", Getter::class.get().call(KImpl()))

    check("fun test.JSub.get(): kotlin.CharSequence!", JSub::class, isAbstract = false)
    assertEquals(JSub::get, JSub::class.get())
    assertEquals("KBase", JSub::class.get().call(JSub()))

    return "OK"
}
