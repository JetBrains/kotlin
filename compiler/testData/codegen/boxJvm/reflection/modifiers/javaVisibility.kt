// TARGET_BACKEND: JVM

// WITH_REFLECT
// FILE: J.java

class J {
    protected class C {}
    protected static class D {}

    void foo() {}
    protected void bar() {}
    protected static void baz() {}
}

// FILE: JMembers.java

public class JMembers {
    public String publicMethod() { return "JMembers.publicMethod"; }
    private String privateMethod() { return "JMembers.privateMethod"; }
    public String publicField = "JMembers.publicField";
    private String privateField = "JMembers.privateField";
}

// FILE: JInterface.java

public interface JInterface {
    String abstractMethod();
    default String defaultMethod() { return "JInterface.defaultMethod"; }
    static String staticMethod() { return "JInterface.staticMethod"; }
}

// FILE: JInterfaceImpl.java

public class JInterfaceImpl implements JInterface {
    public String abstractMethod() { return "JInterfaceImpl.abstractMethod"; }
}

// FILE: JProtected.java

public class JProtected {
    protected String protectedMethod() { return "JProtected.protectedMethod"; }
    protected String inheritedProtectedMethod() { return "JProtected.inheritedProtectedMethod"; }
}

// FILE: JWidens.java

public class JWidens extends JProtected {
    public String protectedMethod() { return "JWidens.protectedMethod"; }
}

// FILE: JFakeOverride.java

public class JFakeOverride extends JProtected {}

// FILE: K.kt

// Visibility of Java callables. See callableVisibility.kt for Kotlin callables and callableModifiersInMixedHierarchies.kt for
// Java callables inherited by Kotlin classes and vice versa.

import kotlin.reflect.KClass
import kotlin.reflect.KCallable
import kotlin.reflect.KVisibility
import kotlin.reflect.full.IllegalCallableAccessException
import kotlin.reflect.full.staticFunctions
import kotlin.reflect.jvm.isAccessible
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

private fun KClass<*>.member(name: String): KCallable<*> = members.single { it.name == name }

// Private and protected callables are not accessible by default, and can only be called after `isAccessible = true`.
private fun checkCallRequiresAccess(expected: Any?, callable: KCallable<*>, vararg args: Any?) {
    assertFailsWith<IllegalCallableAccessException> { callable.call(*args) }
    callable.isAccessible = true
    assertEquals(expected, callable.call(*args))
}

fun box(): String {
    // Package-private class
    assertEquals(null, J::class.visibility)
    // Protected+package class
    assertEquals(null, J.C::class.visibility)
    // Protected static class
    assertEquals(null, J.D::class.visibility)

    // Package-private method
    assertEquals(null, J::foo.visibility)
    // Protected+package method
    assertEquals(null, J::bar.visibility)
    // Protected static method
    assertEquals(null, J::baz.visibility)

    JMembers::class.member("publicMethod").let {
        assertEquals(KVisibility.PUBLIC, it.visibility)
        assertEquals("JMembers.publicMethod", it.call(JMembers()))
    }
    JMembers::class.member("privateMethod").let {
        assertEquals(KVisibility.PRIVATE, it.visibility)
        checkCallRequiresAccess("JMembers.privateMethod", it, JMembers())
    }
    JMembers::class.member("publicField").let {
        assertEquals(KVisibility.PUBLIC, it.visibility)
        assertEquals("JMembers.publicField", it.call(JMembers()))
    }
    JMembers::class.member("privateField").let {
        assertEquals(KVisibility.PRIVATE, it.visibility)
        checkCallRequiresAccess("JMembers.privateField", it, JMembers())
    }

    // Interface members are implicitly public.
    JInterface::class.member("abstractMethod").let {
        assertEquals(KVisibility.PUBLIC, it.visibility)
        assertEquals("JInterfaceImpl.abstractMethod", it.call(JInterfaceImpl()))
    }
    JInterface::class.member("defaultMethod").let {
        assertEquals(KVisibility.PUBLIC, it.visibility)
        assertEquals("JInterface.defaultMethod", it.call(JInterfaceImpl()))
    }
    JInterface::class.staticFunctions.single().let {
        assertEquals(KVisibility.PUBLIC, it.visibility)
        assertEquals("JInterface.staticMethod", it.call())
    }

    JProtected::class.member("protectedMethod").let {
        checkCallRequiresAccess("JProtected.protectedMethod", it, JProtected())
    }
    JWidens::class.member("protectedMethod").let {
        assertEquals(KVisibility.PUBLIC, it.visibility)
        assertEquals("JWidens.protectedMethod", it.call(JWidens()))
    }
    // A fake override keeps the visibility of the inherited method. Note that it's a different method from the one above, because
    // both callables use the same Java method, and `isAccessible = true` above would have made it accessible.
    JFakeOverride::class.member("inheritedProtectedMethod").let {
        assertEquals(null, it.visibility)
        checkCallRequiresAccess("JProtected.inheritedProtectedMethod", it, JFakeOverride())
    }

    return "OK"
}
