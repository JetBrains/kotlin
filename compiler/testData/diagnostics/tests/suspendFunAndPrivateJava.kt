// FIR_IDENTICAL
// ISSUE: KT-61076
// FILE: InspectionApplicationBase.java

public class InspectionApplicationBase {
    private String loadInspectionProfile() { return ""; }
}

// FILE: Main.kt

class QodanaInspectionApplication: InspectionApplicationBase() {
    suspend fun loadInspectionProfile(): String = ""
}
