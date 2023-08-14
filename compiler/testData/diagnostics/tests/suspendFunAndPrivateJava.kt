// FIR_IDENTICAL
// ISSUE: KT-61076
// FILE: InspectionApplicationBase.java

public class InspectionApplicationBase {
    private String loadInspectionProfile() { return ""; }
}

// FILE: Main.kt

class QodanaInspectionApplication: InspectionApplicationBase() {
    // K1: ok
    // K2: CONFLICTING_OVERLOADS (suspend fun loadInspectionProfile(): String, fun loadInspectionProfile(): String!)
    suspend fun loadInspectionProfile(): String = ""
}
