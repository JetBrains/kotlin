// PLATFORM_DEPENDANT_METADATA
// ALLOW_AST_ACCESS
// TARGET_BACKEND: JVM_IR
package test

import java.io.Serializable

@Target(AnnotationTarget.TYPE)
annotation class A

interface Foo<T : @A Number> : @A Serializable {
    fun <E, F : @A E> bar()
}
