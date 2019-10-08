@Target(AnnotationTarget.TYPEALIAS)
annotation class Anno

@Anno
private typealias Foo = String

// TESTED_OBJECT_KIND: function
// TESTED_OBJECTS: PrivateTypealiasKt, Foo$annotations
// FLAGS: ACC_DEPRECATED, ACC_STATIC, ACC_SYNTHETIC, ACC_PRIVATE
