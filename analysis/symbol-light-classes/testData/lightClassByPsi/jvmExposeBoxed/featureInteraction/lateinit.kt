// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
// JVM_EXPOSE_BOXED
// COMPILATION_ERRORS

@JvmInline
value class StringWrapper(val s: String)

class Bar {
    lateinit var foo: StringWrapper
}
