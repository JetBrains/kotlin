//ALLOW_AST_ACCESS
package test

import java.lang.annotation.ElementType

annotation class Anno(vararg t: ElementType)

Anno(ElementType.METHOD, ElementType.FIELD) fun foo() {}

Anno(ElementType.PACKAGE) val bar = 42

Anno() fun baz() {}
