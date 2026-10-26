// LANGUAGE: +FullValueClasses
// TARGET_PLATFORM: JVM
// WITH_STDLIB

inline value class ExplicitInlineValueClass(val value: Int)

value inline class ReversedModifiers(val value: String)

@JvmInline
inline value class AnnotatedExplicitInlineValueClass(val value: Long)
