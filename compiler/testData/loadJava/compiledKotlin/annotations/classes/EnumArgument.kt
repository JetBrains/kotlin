//ALLOW_AST_ACCESS
package test

import java.lang.annotation.ElementType

annotation class Anno(val t: ElementType)

Anno(ElementType.METHOD) class Class {
    Anno(ElementType.PARAMETER) inner class Inner
    
    Anno(ElementType.TYPE) class Nested

    Anno(ElementType.ANNOTATION_TYPE) default object
}
