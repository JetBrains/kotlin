// IGNORE_LIGHT_ANALYSIS
// TARGET_BACKEND: JVM
// FILE: ArrayAnnEnumJava.java
package light.ann.array;

import static light.ann.array.AnnAuxEnum.ANN_ENUM_VAL_A;
import static light.ann.array.AnnAuxEnum.ANN_ENUM_VAL_B;

public @interface ArrayAnnEnumJava {
    AnnAuxEnum[] enumValDef() default { ANN_ENUM_VAL_A, ANN_ENUM_VAL_B };
}

// FILE: ArrayAnnUsage.kt
package light.ann.array

import light.ann.array.AnnAuxEnum.ANN_ENUM_VAL_A;
import light.ann.array.AnnAuxEnum.ANN_ENUM_VAL_B;

enum class AnnAuxEnum { ANN_ENUM_VAL_A, ANN_ENUM_VAL_B }

@ArrayAnnEnumJava(enumValDef = arrayOf(ANN_ENUM_VAL_A, ANN_ENUM_VAL_B))
fun box() = "OK"
