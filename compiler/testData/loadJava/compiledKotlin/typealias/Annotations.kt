// PLATFORM_DEPENDANT_METADATA
//ALLOW_AST_ACCESS
// NO_CHECK_SOURCE_VS_BINARY
//^ While compiling source, we do not store annotation default values, but we load them when reading compiled files
package test

@Target(AnnotationTarget.TYPEALIAS)
annotation class Ann(val value: String = "")

@Ann()
typealias A1 = String
@Ann("OK")
typealias A2 = String
