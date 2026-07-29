// VALHALLA_SUPPORT: PRIMITIVES_AND_FULL_VALUE_CLASSES
// LANGUAGE: +FullValueClasses

// MODULE: lib
// FILE: JavaVal.java
public value record JavaVal(int x) {}


// MODULE: main(lib)
// CHECK_BYTECODE_TEXT
// FILE: javaValueClassFieldBinary.kt
class KotlinHolder(val v: JavaVal)

fun box(): String {
    val holder = KotlinHolder(JavaVal(7))
    if (holder.v.x != 7) return "x=${holder.v.x}"
    return "OK"
}

// 1 ATTRIBUTE LoadableDescriptors : LJavaVal;\n
