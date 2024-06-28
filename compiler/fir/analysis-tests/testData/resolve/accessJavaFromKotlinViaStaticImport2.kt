// FILE: useSite.kt

import InspectionProfileImpl.INIT_INSPECTIONS

fun foo(): Int = 4

// FILE: InspectionProfileImpl.java
import static Configuration.StaticConfigurationClass

public class InspectionProfileImpl extends InspectionProfile<StaticConfigurationClass> {
    public static boolean INIT_INSPECTIONS;
}

// FILE: InspectionProfile.java
public class InspectionProfile <T> {
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
