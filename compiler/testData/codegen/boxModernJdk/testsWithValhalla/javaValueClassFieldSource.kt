// VALHALLA_SUPPORT: PRIMITIVES_AND_FULL_VALUE_CLASSES
// LANGUAGE: +FullValueClasses
// CHECK_BYTECODE_TEXT

// FILE: JavaVal.java
public value record JavaVal(int x) {}

// FILE: javaValueClassFieldSource.kt
class KotlinHolder(val v: JavaVal)

fun box(): String {
    val holder = KotlinHolder(JavaVal(42))
    if (holder.v.x != 42) return "x=${holder.v.x}"
    return "OK"
}

// 1 ATTRIBUTE LoadableDescriptors : LJavaVal;\n
