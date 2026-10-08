package kotlin.internal

// This annotation is not part of stdlib before v2.5
// It's needed by grouping testinfra v2.5+ for backward compatibility testing against Kotlin versions less then v2.5.
// Unlike in stdlib, it is public: the K1 frontend of 1.9.x compilers reports an unsuppressible visibility error
// for an internal class of another module, even with the `INVISIBLE_REFERENCE` suppression the testinfra adds.
@Target(AnnotationTarget.FILE)
@Retention(AnnotationRetention.BINARY)
annotation class ReflectionPackageName(val name: String)
