// TARGET_BACKEND: JVM
// ALLOW_AST_ACCESS
package test

import java.lang.annotation.ElementType

annotation class Anno(val t: ElementType)

@Anno(ElementType.METHOD) fun foo() {}

@field:Anno(ElementType.FIELD) val bar = { 42 }()
