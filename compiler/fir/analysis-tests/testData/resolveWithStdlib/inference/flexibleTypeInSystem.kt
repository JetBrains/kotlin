// ISSUE: KT-37623
// FULL_JDK
// FILE: JavaEnum.java

public enum JavaEnum {
    A;
}

// FILE: main.kt

import java.util.EnumMap

val enumMap = EnumMap(
    mapOf(
        JavaEnum.A to "A"
    )
)