// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: JBase.java

public abstract class JBase {
    public String openM() { return "JBase.openM"; }
    public final String finalM() { return "JBase.finalM"; }
    public abstract String abstractM();
    public static String staticM() { return "JBase.staticM"; }
}

// FILE: JFakeOverrides.java

public class JFakeOverrides extends JBase {
    public String abstractM() { return "JFakeOverrides.abstractM"; }
}

// FILE: JOpenOverride.java

public class JOpenOverride extends JBase {
    public String openM() { return "JOpenOverride.openM"; }
    public String abstractM() { return "JOpenOverride.abstractM"; }
}

// FILE: JFinalOverride.java

public class JFinalOverride extends JBase {
    public final String openM() { return "JFinalOverride.openM"; }
    public String abstractM() { return "JFinalOverride.abstractM"; }
}

// FILE: JAbstractOverride.java

public abstract class JAbstractOverride extends JBase {
    public abstract String openM();
}

// FILE: JAbstractOverrideImpl.java

public class JAbstractOverrideImpl extends JAbstractOverride {
    public String openM() { return "JAbstractOverrideImpl.openM"; }
    public String abstractM() { return "JAbstractOverrideImpl.abstractM"; }
}

// FILE: JFinal.java

public final class JFinal extends JBase {
    public String openM() { return "JFinal.openM"; }
    public String abstractM() { return "JFinal.abstractM"; }
}

// FILE: JInterface.java

public interface JInterface {
    String abstractM();
    default String defaultM() { return "JInterface.defaultM"; }
    static String staticM() { return "JInterface.staticM"; }
}

// FILE: JFakeOverridesInSubinterface.java

public interface JFakeOverridesInSubinterface extends JInterface {}

// FILE: JOverrideInSubinterface.java

public interface JOverrideInSubinterface extends JInterface {
    default String defaultM() { return "JOverrideInSubinterface.defaultM"; }
}

// FILE: JAbstractOverrideInSubinterface.java

public interface JAbstractOverrideInSubinterface extends JInterface {
    String defaultM();
}

// FILE: JFakeOverridesInSubinterfaceImpl.java

public class JFakeOverridesInSubinterfaceImpl implements JFakeOverridesInSubinterface {
    public String abstractM() { return "JFakeOverridesInSubinterfaceImpl.abstractM"; }
}

// FILE: JOverrideInSubinterfaceImpl.java

public class JOverrideInSubinterfaceImpl implements JOverrideInSubinterface {
    public String abstractM() { return "JOverrideInSubinterfaceImpl.abstractM"; }
}

// FILE: JAbstractOverrideInSubinterfaceImpl.java

public class JAbstractOverrideInSubinterfaceImpl implements JAbstractOverrideInSubinterface {
    public String abstractM() { return "JAbstractOverrideInSubinterfaceImpl.abstractM"; }
    public String defaultM() { return "JAbstractOverrideInSubinterfaceImpl.defaultM"; }
}

// FILE: box.kt

// Modality of Java callables. See callableModality.kt for Kotlin callables and callableModifiersInMixedHierarchies.kt for
// Java callables inherited by Kotlin classes and vice versa.

import kotlin.reflect.KClass
import kotlin.reflect.KCallable
import kotlin.reflect.full.staticFunctions
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

private fun KClass<*>.member(name: String): KCallable<*> = members.single { it.name == name }

private fun checkFinal(callable: KCallable<*>) {
    assertTrue(callable.isFinal)
    assertFalse(callable.isOpen)
    assertFalse(callable.isAbstract)
}

private fun checkOpen(callable: KCallable<*>) {
    assertFalse(callable.isFinal)
    assertTrue(callable.isOpen)
    assertFalse(callable.isAbstract)
}

private fun checkAbstract(callable: KCallable<*>) {
    assertFalse(callable.isFinal)
    assertFalse(callable.isOpen)
    assertTrue(callable.isAbstract)
}

// TODO: this is a bug. Static methods and methods of final classes cannot be overridden, so they must be final, but their modality
//  is currently computed only from the method's own modifiers. Replace with `checkFinal` once fixed.
private fun checkOpenButShouldBeFinal(callable: KCallable<*>) {
    checkOpen(callable)
}

fun box(): String {
    JBase::class.member("openM").let {
        checkOpen(it)
        assertEquals("JOpenOverride.openM", it.call(JOpenOverride()))
    }
    JBase::class.member("finalM").let {
        checkFinal(it)
        assertEquals("JBase.finalM", it.call(JOpenOverride()))
    }
    JBase::class.member("abstractM").let {
        checkAbstract(it)
        assertEquals("JOpenOverride.abstractM", it.call(JOpenOverride()))
    }
    JBase::class.staticFunctions.single().let {
        checkOpenButShouldBeFinal(it)
        assertEquals("JBase.staticM", it.call())
    }

    // Overrides.
    JOpenOverride::class.member("openM").let {
        checkOpen(it)
        assertEquals("JOpenOverride.openM", it.call(JOpenOverride()))
    }
    // An override of an abstract method is open unless it is explicitly final.
    JOpenOverride::class.member("abstractM").let {
        checkOpen(it)
        assertEquals("JOpenOverride.abstractM", it.call(JOpenOverride()))
    }
    JFinalOverride::class.member("openM").let {
        checkFinal(it)
        assertEquals("JFinalOverride.openM", it.call(JFinalOverride()))
    }
    JAbstractOverride::class.member("openM").let {
        checkAbstract(it)
        assertEquals("JAbstractOverrideImpl.openM", it.call(JAbstractOverrideImpl()))
    }
    JFinal::class.member("openM").let {
        checkOpenButShouldBeFinal(it)
        assertEquals("JFinal.openM", it.call(JFinal()))
    }

    // Fake overrides keep the modality of the inherited method.
    JFakeOverrides::class.member("openM").let {
        checkOpen(it)
        assertEquals("JBase.openM", it.call(JFakeOverrides()))
    }
    JFakeOverrides::class.member("finalM").let {
        checkFinal(it)
        assertEquals("JBase.finalM", it.call(JFakeOverrides()))
    }
    JFinal::class.member("finalM").let {
        checkFinal(it)
        assertEquals("JBase.finalM", it.call(JFinal()))
    }
    // Unlike in interfaces, static methods of Java classes are inherited.
    JFakeOverrides::class.staticFunctions.single().let {
        checkOpenButShouldBeFinal(it)
        assertEquals("JBase.staticM", it.call())
    }

    // Interfaces.
    JInterface::class.member("abstractM").let {
        checkAbstract(it)
        assertEquals("JFakeOverridesInSubinterfaceImpl.abstractM", it.call(JFakeOverridesInSubinterfaceImpl()))
    }
    JInterface::class.member("defaultM").let {
        checkOpen(it)
        assertEquals("JInterface.defaultM", it.call(JFakeOverridesInSubinterfaceImpl()))
        assertEquals("JAbstractOverrideInSubinterfaceImpl.defaultM", it.call(JAbstractOverrideInSubinterfaceImpl()))
    }
    JInterface::class.staticFunctions.single().let {
        checkOpenButShouldBeFinal(it)
        assertEquals("JInterface.staticM", it.call())
    }

    // Subinterfaces. Static interface methods are not inherited.
    assertEquals(emptyList(), JFakeOverridesInSubinterface::class.staticFunctions)
    JFakeOverridesInSubinterface::class.member("abstractM").let {
        checkAbstract(it)
        assertEquals("JFakeOverridesInSubinterfaceImpl.abstractM", it.call(JFakeOverridesInSubinterfaceImpl()))
    }
    JFakeOverridesInSubinterface::class.member("defaultM").let {
        checkOpen(it)
        assertEquals("JInterface.defaultM", it.call(JFakeOverridesInSubinterfaceImpl()))
    }
    JOverrideInSubinterface::class.member("defaultM").let {
        checkOpen(it)
        assertEquals("JOverrideInSubinterface.defaultM", it.call(JOverrideInSubinterfaceImpl()))
    }
    JAbstractOverrideInSubinterface::class.member("defaultM").let {
        checkAbstract(it)
        assertEquals("JAbstractOverrideInSubinterfaceImpl.defaultM", it.call(JAbstractOverrideInSubinterfaceImpl()))
    }

    return "OK"
}
