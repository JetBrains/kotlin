// !DIAGNOSTICS: -UNUSED_PARAMETER

// KT-4034 An expression of type Nothing may not affect 'definite return' analysis

interface JavaClassifierType
interface TypeUsage
interface JetType

private fun transformClassifierType(classifierType: JavaClassifierType, howThisTypeIsUsed: TypeUsage): JetType? {
    null!!
}
