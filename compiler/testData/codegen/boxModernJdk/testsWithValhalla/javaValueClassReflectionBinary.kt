// VALHALLA_VALUE_CLASSES
// LANGUAGE: +FullValueClasses
// WITH_REFLECT

// MODULE: lib
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

// MODULE: main(lib)
// FILE: javaValueClassReflectionBinary.kt
import kotlin.reflect.full.functions
import kotlin.reflect.full.instanceParameter

fun box(): String {
    if (!JavaVal::class.isValue) return "FAIL: JavaVal::class.isValue should be true"

    // Java interfaces, enums and annotations without Kotlin metadata are not value classes.
    if (JavaIface::class.isValue) return "FAIL: a Java interface must not be a value class"
    if (JavaEnum::class.isValue) return "FAIL: a Java enum must not be a value class"
    if (JavaAnno::class.isValue) return "FAIL: a Java annotation must not be a value class"

    val v = JavaVal(7)

    val toStringFun = JavaVal::class.functions.first { it.name == "toString" && it.parameters.size == 1 }
    val ts = toStringFun.call(v) as String
    if (ts != v.toString()) return "FAIL toString via call: $ts"

    val doubled = JavaVal::class.functions.first { it.name == "doubled" }
    if (doubled.call(v) != 14) return "FAIL doubled via call: ${doubled.call(v)}"
    if (doubled.callBy(mapOf(doubled.instanceParameter!! to v)) != 14) return "FAIL doubled via callBy"

    return "OK"
}
