package kotlin.internal

// This annotation is not part of stdlib before v2.5
// It's needed by grouping testinfra v2.5+ for backward compatibility testing against Kotlin versions less then v2.5.
@Target(AnnotationTarget.FILE)
@Retention(AnnotationRetention.BINARY)
internal annotation class ReflectionPackageName(val name: String)
