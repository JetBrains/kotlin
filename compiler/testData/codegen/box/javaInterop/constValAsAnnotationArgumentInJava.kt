// WITH_STDLIB
// TARGET_BACKEND: JVM
// IGNORE_BACKEND_K2: JVM_IR
// ISSUE: KT-83570

// FILE: example/KotlinDtoMapping.kt
package example

object KotlinDtoMapping {
    const val ID: String = "id"
}

// Does not matter if defined in Kotlin or in Java
annotation class SimpleAnnotation(val value: String)

// FILE: example/AbstractJavaDto.java
package example;

import static example.KotlinDtoMapping.ID;

public abstract class AbstractJavaDto {
    @SimpleAnnotation(ID)
    public String getId() {
        return "OK";
    }
}

// FILE: main.kt
package example

class KotlinDto : AbstractJavaDto()

fun box(): String {
    val dto = KotlinDto()
    return dto.getId()
}
