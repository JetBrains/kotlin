//ALLOW_AST_ACCESS
package test

annotation class Anno(val value: String)

class Constructor [Anno(value = "string")]()
