// TARGET_BACKEND: JVM
// SKIP_KT_DUMP
// FULL_JDK

interface KotlinList<T> : java.util.List<T>

interface SpecificList : KotlinList<String>
