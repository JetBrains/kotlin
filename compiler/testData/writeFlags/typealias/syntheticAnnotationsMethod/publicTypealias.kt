@Target(AnnotationTarget.TYPEALIAS)
annotation class Anno

@Anno
typealias Foo = String

// TESTED_OBJECT_KIND: function
// TESTED_OBJECTS: PublicTypealiasKt, Foo$annotations
// FLAGS: ACC_DEPRECATED, ACC_STATIC, ACC_SYNTHETIC, ACC_PUBLIC
