// LOOK_UP_FOR_ELEMENT_OF_TYPE: org.jetbrains.kotlin.psi.KtProperty
// FILE: KotlinFile.kt
package one.three

import one.four.Manager

class KotlinFile {
    <expr>private val branchManager: Manager? = null</expr>
}

// FILE: one/four/Manager.java
package one.four;

import one.two.Service;
import static one.three.KotlinFile.*;

@Service(Service.Level.PROJECT)
public class Manager {
}

// FILE: one/two/Service.java
package one.two;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public @interface Service {
    Level[] value() default Level.APP;

    enum Level {APP, PROJECT}
}