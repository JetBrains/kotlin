// LIBRARY_PLATFORMS: JVM
package test

@Target(AnnotationTarget.PROPERTY)
annotation class OnProperty

class A {
    @property:OnProperty
    @JvmField
    var withPropertyAnnotation: Int = 1

    @JvmField
    var plain: Int = 2
}
