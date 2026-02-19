// one.DelegatedPropertyKt
// WITH_STDLIB
package one

@Target(AnnotationTarget.FIELD)
annotation class MyAnno

@delegate:MyAnno
val propertyWithExplicitUseSite by lazy { 0 }
