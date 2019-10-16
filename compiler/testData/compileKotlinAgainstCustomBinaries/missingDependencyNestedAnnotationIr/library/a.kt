package a

import kotlin.reflect.KClass

interface A {
    @Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.TYPE_PARAMETER)
    annotation class Anno(val value: String)
}

annotation class K(val klass: KClass<*>)
