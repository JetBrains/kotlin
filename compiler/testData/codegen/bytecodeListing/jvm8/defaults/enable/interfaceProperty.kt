// !JVM_DEFAULT_MODE: enable
// JVM_TARGET: 1.8
// WITH_STDLIB
@Target(AnnotationTarget.PROPERTY)
annotation class Foo

interface Deprecated {

    @Foo
    val prop: String
}
