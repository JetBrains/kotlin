//ALLOW_AST_ACCESS
package test

@Target(AnnotationTarget.TYPEALIAS)
annotation class Ann(val value: String = "")

@Ann()
typealias A1 = String
@Ann("OK")
typealias A2 = String
