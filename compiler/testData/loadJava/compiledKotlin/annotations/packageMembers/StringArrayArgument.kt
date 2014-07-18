//ALLOW_AST_ACCESS
package test

annotation class Anno(vararg t: String)

Anno("live", "long") fun foo() {}

Anno("prosper") val bar = 42

Anno() fun baz() {}
