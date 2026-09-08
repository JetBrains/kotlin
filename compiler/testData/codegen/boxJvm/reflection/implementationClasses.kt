// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: test/J.java
package test;

public class J {
    public String memberField = "";
    public static String staticField = "";
    public static final String staticFinalField = "";

    public J(String value) {}

    public void memberFun() {}

    public static void staticFun() {}
}

// FILE: test/JOverride.java
package test;

public class JOverride extends KBase {
    private String storage;

    @Override
    public String getOverriddenVal() {
        return storage;
    }

    @Override
    public String getOverriddenVar() {
        return storage;
    }

    @Override
    public void setOverriddenVar(String value) {
        storage = value;
    }
}

// FILE: test/JEnum.java
package test;

public enum JEnum {
    ENTRY
}

// FILE: test/JAnno.java
package test;

public @interface JAnno {
    String value();
}

// FILE: test.kt
package test

import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

class K(val constructorVal: Int) {
    val memberVal: String = ""
    var memberVar: String = ""
    fun memberFun() {}
}

abstract class KBase {
    abstract val overriddenVal: String?
    abstract var overriddenVar: String?
}

val topLevelVal = ""
var topLevelVar = ""
fun topLevelFun() {}

private val systemProperties = Class.forName("kotlin.reflect.jvm.internal.SystemPropertiesKt")
private val useK1 = systemProperties.getMethod("getUseK1Implementation").invoke(null) == true
private val useK1ForMembers = useK1 || systemProperties.getMethod("getUseK1ImplementationForMembers").invoke(null) == true

private val errors = StringBuilder()

private fun implementationClassName(callable: KCallable<*>): String {
    var result: Any = callable
    if (result is kotlin.jvm.internal.CallableReference) {
        result = result.compute()
    }
    // Property references are wrapped in kotlin.reflect.jvm.internal.LazyKPropertyN/LazyKMutablePropertyN.
    while (result.javaClass.simpleName.startsWith("LazyK")) {
        val getDelegate = result.javaClass.methods.single { method ->
            method.name == "getDelegate" && method.parameterTypes.isEmpty() && KProperty::class.java.isAssignableFrom(method.returnType)
        }
        result = getDelegate.invoke(result)!!
    }
    return result.javaClass.simpleName
}

private fun check(description: String, callable: KCallable<*>, new: String, k1ForMembers: String = new, k1: String) {
    val expected = when {
        useK1 -> k1
        useK1ForMembers -> k1ForMembers
        else -> new
    }
    val actual = implementationClassName(callable)
    if (expected != actual) {
        errors.append("$description: expected $expected, actual $actual\n")
    }
}

private fun member(klass: KClass<*>, name: String): KCallable<*> = klass.members.single { it.name == name }

fun box(): String {
    // Kotlin member properties and functions.
    check("K::memberVal", K::memberVal, new = "KotlinKProperty1", k1ForMembers = "DescriptorKProperty1", k1 = "DescriptorKProperty1")
    check(
        "K::memberVar", K::memberVar,
        new = "KotlinKMutableProperty1", k1ForMembers = "DescriptorKMutableProperty1", k1 = "DescriptorKMutableProperty1",
    )
    check("K::memberFun", K::memberFun, new = "KotlinKNamedFunction", k1ForMembers = "DescriptorKFunction", k1 = "DescriptorKFunction")
    check(
        "K::class.members['memberVal']", member(K::class, "memberVal"),
        new = "KotlinKProperty1", k1ForMembers = "DescriptorKProperty1", k1 = "DescriptorKProperty1",
    )
    check(
        "K::class.members['memberVar']", member(K::class, "memberVar"),
        new = "KotlinKMutableProperty1", k1ForMembers = "DescriptorKMutableProperty1", k1 = "DescriptorKMutableProperty1",
    )
    check(
        "K::class.members['memberFun']", member(K::class, "memberFun"),
        new = "KotlinKNamedFunction", k1ForMembers = "DescriptorKFunction", k1 = "DescriptorKFunction",
    )

    // Kotlin constructors.
    check("::K", ::K, new = "KotlinKConstructor", k1 = "DescriptorKFunction")
    check("K::class.constructors", K::class.constructors.single(), new = "KotlinKConstructor", k1 = "DescriptorKFunction")

    // Kotlin top-level properties and functions.
    check("::topLevelVal", ::topLevelVal, new = "KotlinKProperty0", k1 = "DescriptorKProperty0")
    check("::topLevelVar", ::topLevelVar, new = "KotlinKMutableProperty0", k1 = "DescriptorKMutableProperty0")
    check("::topLevelFun", ::topLevelFun, new = "KotlinKNamedFunction", k1 = "DescriptorKFunction")

    // Java member properties and functions.
    check(
        "J::memberField", J::memberField,
        new = "JavaFieldKMutableProperty1", k1ForMembers = "DescriptorKMutableProperty1", k1 = "DescriptorKMutableProperty1",
    )
    check("J::memberFun", J::memberFun, new = "JavaKNamedFunction", k1 = "DescriptorKFunction")
    check(
        "J::class.members['memberField']", member(J::class, "memberField"),
        new = "JavaFieldKMutableProperty1", k1ForMembers = "DescriptorKMutableProperty1", k1 = "DescriptorKMutableProperty1",
    )
    check("J::class.members['memberFun']", member(J::class, "memberFun"), new = "JavaKNamedFunction", k1 = "DescriptorKFunction")

    // Java constructors.
    check("::J", ::J, new = "JavaKConstructor", k1 = "DescriptorKFunction")
    check("J::class.constructors", J::class.constructors.single(), new = "JavaKConstructor", k1 = "DescriptorKFunction")

    // Java static properties and functions.
    check("J::staticField", J::staticField, new = "JavaFieldKMutableProperty0", k1 = "DescriptorKMutableProperty0")
    check("J::staticFinalField", J::staticFinalField, new = "JavaFieldKProperty0", k1 = "DescriptorKProperty0")
    check("J::staticFun", J::staticFun, new = "JavaKNamedFunction", k1 = "DescriptorKFunction")
    check(
        "J::class.members['staticField']", member(J::class, "staticField"),
        new = "JavaFieldKMutableProperty0", k1 = "DescriptorKMutableProperty0",
    )
    check(
        "J::class.members['staticFinalField']", member(J::class, "staticFinalField"),
        new = "JavaFieldKProperty0", k1 = "DescriptorKProperty0",
    )
    check("J::class.members['staticFun']", member(J::class, "staticFun"), new = "JavaKNamedFunction", k1 = "DescriptorKFunction")

    // Properties declared in Kotlin and overridden by methods in Java.
    // Note that callable references to such properties are not supported yet, see KT-87863.
    check(
        "JOverride::class.members['overriddenVal']", member(JOverride::class, "overriddenVal"),
        new = "JavaForKotlinOverrideKProperty1", k1ForMembers = "DescriptorKProperty1", k1 = "DescriptorKProperty1",
    )
    check(
        "JOverride::class.members['overriddenVar']", member(JOverride::class, "overriddenVar"),
        new = "JavaForKotlinOverrideKMutableProperty1", k1ForMembers = "DescriptorKMutableProperty1", k1 = "DescriptorKMutableProperty1",
    )

    // Java annotation methods, represented as properties.
    // Note that `JAnno::value` is a reference to a synthetic Java property, which does not use kotlin-reflect at all, see KT-55980.
    check(
        "JAnno::class.members['value']", member(JAnno::class, "value"),
        new = "JavaAnnotationMethodKProperty1", k1ForMembers = "DescriptorKProperty1", k1 = "DescriptorKProperty1",
    )

    // The synthetic `entries` property of a Java enum class.
    check("JEnum::entries", JEnum::entries, new = "JavaEnumEntriesKProperty", k1 = "DescriptorKProperty0")
    check(
        "JEnum::class.members['entries']", member(JEnum::class, "entries"),
        new = "JavaEnumEntriesKProperty", k1 = "DescriptorKProperty0",
    )

    return if (errors.isEmpty()) "OK" else "Fail:\n$errors"
}
