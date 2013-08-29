package test

import java.lang.annotation.ElementType

annotation class Anno(t: ElementType)

class Class {
    Anno(ElementType.METHOD) fun foo() {}

    Anno(ElementType.FIELD) var bar = 42
}
