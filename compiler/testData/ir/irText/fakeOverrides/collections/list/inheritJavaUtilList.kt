// IGNORE_BACKEND: JKLIB
// TARGET_BACKEND: JVM
// SKIP_KT_DUMP
// FULL_JDK

// KT-89566 Reflection: original methods from explicit java.lang/java.util supertypes (instead of Kotlin mapped builtin classes) are absent in the new implementation
// KOTLIN_REFLECT_DUMP_MISMATCH

interface KotlinList<T> : java.util.List<T>

interface SpecificList : KotlinList<String>
