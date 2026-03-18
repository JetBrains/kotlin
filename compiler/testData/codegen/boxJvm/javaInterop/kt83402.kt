// WITH_STDLIB
// TARGET_BACKEND: JVM

// FILE: example/KotlinDtoMapping.kt
package example

object KotlinDtoMapping {
    const val ID: String = "id"
}

// FILE: example/SimpleAnnotation.java
package example;

public @interface SimpleAnnotation {
    String value() default "ok";
}

// FILE: example/AbstractJavaDto.java
package example;

import static example.KotlinDtoMapping.ID;

public abstract class AbstractJavaDto {
    protected String id;

    @SimpleAnnotation(ID)
    public String getId() {
        return id;
    }
}

// FILE: main.kt
package example

class KotlinDto : AbstractJavaDto()

fun box(): String {
    val dto = KotlinDto()
    return "OK"
}
