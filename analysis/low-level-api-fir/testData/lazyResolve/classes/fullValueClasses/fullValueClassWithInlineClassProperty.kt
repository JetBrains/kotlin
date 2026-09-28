// LANGUAGE: +FullValueClasses
// SKIP_WHEN_OUT_OF_CONTENT_ROOT
package pack

@JvmInline
value class Inline(val value: Int)

value class Fu<caret>ll(val inline: Inline, val nullableInline: Inline?)
