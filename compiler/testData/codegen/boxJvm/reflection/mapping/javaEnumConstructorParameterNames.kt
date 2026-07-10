// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: ColorEnum.java
public enum ColorEnum {
    RED(255, 0, 0);

    ColorEnum(int r, int g, int b) {}
}

// FILE: Plain.java
public class Plain {
    public Plain(int r, int g) {}

    public class Inner {
        public Inner(int x) {}
    }
}

// FILE: box.kt
// Java classes here are compiled without `-parameters`, so real parameter names are not available (see realParameterNames.kt
// for the case with `-parameters`). Synthetic JVM parameters (enum name/ordinal, outer instance) must not leak into Kotlin parameters.

import kotlin.test.assertEquals

fun box(): String {
    val enumCtor = ColorEnum::class.constructors.single()
    assertEquals(3, enumCtor.parameters.size)
    // TODO(KT-82784): names should be null. Note that the synthetic names are counted from the JVM signature, which starts with
    //  the synthetic `name` and `ordinal` parameters of the enum constructor.
    assertEquals(listOf("arg2", "arg3", "arg4"), enumCtor.parameters.map { it.name })

    // TODO(KT-82784): names should be null.
    assertEquals(listOf("arg0", "arg1"), Plain::class.constructors.single().parameters.map { it.name })
    assertEquals(listOf(null, "arg1"), Plain.Inner::class.constructors.single().parameters.map { it.name })

    return "OK"
}
