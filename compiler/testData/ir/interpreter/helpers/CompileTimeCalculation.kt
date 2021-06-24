package kotlin

/**
 * Specify that marked function is calculated in compile time and it result can be stored as "const val"
 * Must be used only on built ins methods and further will be replaced with "constexpr" modifier
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.BINARY)
public annotation class CompileTimeCalculation