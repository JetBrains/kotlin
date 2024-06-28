// FILE: useSite.kt

import InspectionProfileImpl.StaticConfigurationClass.INIT_INSPECTIONS

fun foo(): Int = 4

// FILE: InspectionProfileImpl.java
import static Configuration.StaticConfigurationClass

public static final class InspectionProfileImpl extends StaticConfigurationClass {
    public abstract sealed class StaticConfigurationClass  {
        public static boolean INIT_INSPECTIONS;
    }
}

// FILE: Configuration.java
public class Configuration implements KotlinInterface {
    public static class StaticConfigurationClass {
    }
}

// FILE: KotlinInterface.kt
interface KotlinInterface {
    var selectedOptions: Int
}
