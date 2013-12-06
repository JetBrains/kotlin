// !DIAGNOSTICS: -UNUSED_PARAMETER

// KT-4034 An expression of type Nothing may not affect 'definite return' analysis

trait JavaClassifierType
trait TypeUsage
trait JetType

private fun transformClassifierType(classifierType: JavaClassifierType, howThisTypeIsUsed: TypeUsage): JetType? {
    null!!
}
