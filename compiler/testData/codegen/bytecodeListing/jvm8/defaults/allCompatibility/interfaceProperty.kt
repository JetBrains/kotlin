// !JVM_DEFAULT_MODE: all-compatibility
// JVM_TARGET: 1.8
// WITH_STDLIB

@Target(AnnotationTarget.PROPERTY)
annotation class Foo

interface Deprecated {

    @Foo
    val prop: String
}
