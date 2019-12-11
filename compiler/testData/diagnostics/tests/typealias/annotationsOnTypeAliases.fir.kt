// WITH_RUNTIME
import kotlin.annotation.AnnotationTarget.*

annotation class NoTarget

@Target(CLASS)
annotation class IrrelevantTarget

@Target(TYPEALIAS)
annotation class TypealiasTarget

@NoTarget
@IrrelevantTarget
@TypealiasTarget
typealias Test = String