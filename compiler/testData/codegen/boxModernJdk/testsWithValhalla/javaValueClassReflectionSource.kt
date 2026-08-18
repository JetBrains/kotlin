// VALHALLA_SUPPORT: PRIMITIVES_AND_FULL_VALUE_CLASSES
// LANGUAGE: +FullValueClasses
// WITH_REFLECT

// FILE: JavaVal.java
public value record JavaVal(int x) {
    public int doubled() { return x * 2; }
}

// FILE: JavaIface.java
public interface JavaIface {}

// FILE: JavaEnum.java
public enum JavaEnum { A }

// FILE: JavaAnno.java
public @interface JavaAnno {}

// FILE: javaValueClassReflectionSource.kt
import kotlin.reflect.full.functions
import kotlin.reflect.full.instanceParameter

fun box(): String {
    if (!JavaVal::class.isValue) return "FAIL: JavaVal::class.isValue should be true"

    // On a Valhalla JDK the raw `Class.isValue()` reports interfaces (and annotations) as value classes, so these guard against a
    // regression where that leaks into `KClass.isValue` for Java classes without Kotlin metadata.
    if (JavaIface::class.isValue) return "FAIL: a Java interface must not be a value class"
    if (JavaEnum::class.isValue) return "FAIL: a Java enum must not be a value class"
    if (JavaAnno::class.isValue) return "FAIL: a Java annotation must not be a value class"

    val v = JavaVal(42)

    val toStringFun = JavaVal::class.functions.first { it.name == "toString" && it.parameters.size == 1 }
    val ts = toStringFun.call(v) as String
    if (ts != v.toString()) return "FAIL toString via call: $ts"

    val doubled = JavaVal::class.functions.first { it.name == "doubled" }
    if (doubled.call(v) != 84) return "FAIL doubled via call: ${doubled.call(v)}"
    if (doubled.callBy(mapOf(doubled.instanceParameter!! to v)) != 84) return "FAIL doubled via callBy"

    return "OK"
}
