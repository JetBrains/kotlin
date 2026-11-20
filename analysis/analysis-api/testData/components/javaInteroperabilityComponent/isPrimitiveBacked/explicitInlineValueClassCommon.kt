// LANGUAGE: +FullValueClasses
// TARGET_PLATFORM: Common

inline value class ExplicitInlineValueClass(val value: Int)

fun consume(value: <expr>ExplicitInlineValueClass</expr>) {}
