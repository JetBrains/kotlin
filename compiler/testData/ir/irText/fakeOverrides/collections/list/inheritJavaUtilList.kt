// TARGET_BACKEND: JVM
// SKIP_KT_DUMP
// FULL_JDK
// SEPARATE_SIGNATURE_DUMP_FOR_K2

interface KotlinList<T> : java.util.List<T>

interface SpecificList : KotlinList<String>
