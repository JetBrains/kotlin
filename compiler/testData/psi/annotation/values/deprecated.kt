// FILE: Simple.kt
@Deprecated("Deprecated", ReplaceWith("NewClass", "foo.bar.baz.NewClass"), DeprecationLevel.HIDDEN)
class Simple

// FILE: Qualified.kt
@kotlin.Deprecated("Deprecated", kotlin.ReplaceWith("NewClass", "foo.bar.baz.NewClass"), level = kotlin.DeprecationLevel.HIDDEN)
class Qualified
