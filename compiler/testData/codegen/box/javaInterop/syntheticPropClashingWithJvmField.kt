// TARGET_BACKEND: JVM
// WITH_STDLIB
// ISSUE: KT-56538

// FILE: SerializableScheme.java

public interface SerializableScheme {
    String getSchemeState();
}

// FILE: NewInspectionProfile.kt

abstract class NewInspectionProfile : SerializableScheme {
    @JvmField
    internal var schemeState: String? = "OK"

    override fun getSchemeState(): String? = schemeState
}

// FILE: InspectionProfileImpl.java

public class InspectionProfileImpl extends NewInspectionProfile {
}

// FILE: InspectionProfileModifiableModel.kt

class InspectionProfileModifiableModel : InspectionProfileImpl()

// FILE: test.kt

fun box(): String {
    return InspectionProfileModifiableModel().schemeState.toString()
}
