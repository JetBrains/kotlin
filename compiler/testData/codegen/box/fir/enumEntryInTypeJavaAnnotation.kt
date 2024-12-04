// ISSUE: KT-58002

// TARGET_BACKEND: JVM_IR

// FILE: NLS.java
import java.lang.annotation.*;

@Target(ElementType.TYPE_USE)
public @interface NLS {
    enum Capitalization { NotSpecified, Specified }
    Capitalization capitalization() default Capitalization.NotSpecified;
}

// FILE: BaseInspection.java

public class BaseInspection {
    @NLS(capitalization = NLS.Capitalization.Specified)
    public static String fetchProbableBugs() {
        return "OK";
    }
}

// FILE: main.kt

fun getGroupDisplayName() = BaseInspection.fetchProbableBugs()
fun box(): String = getGroupDisplayName()
