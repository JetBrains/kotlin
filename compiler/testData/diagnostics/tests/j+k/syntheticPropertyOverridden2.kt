// IGNORE_REVERSED_RESOLVE
// ISSUE: KT-57166

// FILE: Modality.kt
enum class Modality {
    FINAL
}

// FILE: ClassDescriptor.java

public interface ClassDescriptor {
    @NotNull
    Modality getModality();
}

// FILE: DeserializedClassDescriptor.kt

object ProtoEnumFlags {
    fun modality(): Modality = Modality.FINAL
}

class DeserializedClassDescriptor : ClassDescriptor {
    private val modality = ProtoEnumFlags.modality()

    override fun getModality() = modality
}

fun modality(): Modality = Modality.FINAL

class DeserializedClassDescriptor2 : ClassDescriptor {
    private val modality = <!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM_ERROR!><!DEBUG_INFO_MISSING_UNRESOLVED!>modality<!>()<!>

    override fun getModality() = <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>modality<!>
}
