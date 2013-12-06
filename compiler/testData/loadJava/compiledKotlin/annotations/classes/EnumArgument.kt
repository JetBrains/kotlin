package test

import java.lang.annotation.ElementType

annotation class Anno(t: ElementType)

Anno(ElementType.METHOD) class Class {
    Anno(ElementType.PARAMETER) inner class Inner
    
    Anno(ElementType.TYPE) class Nested

    Anno(ElementType.ANNOTATION_TYPE) class object
}
