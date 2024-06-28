// FILE: useSite.kt

fun foo() {
    InspectionProfileImpl.INIT_INSPECTIONS = true
}

// FILE: InspectionProfileImpl.java
public class InspectionProfileImpl extends NewInspectionProfile {
    public static boolean INIT_INSPECTIONS;

    public <T extends InspectionProfileEntry> T getUnwrappedTool() { }
}


// FILE: NewInspectionProfile.kt
import InspectionProfileImpl.INIT_INSPECTIONS

abstract class NewInspectionProfile

// FILE: InspectionProfileEntry.java
public abstract class InspectionProfileEntry {}
