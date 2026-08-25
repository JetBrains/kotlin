// RUN_PIPELINE_TILL: BACKEND
// JDK_KIND: FULL_JDK_11
// ISSUE: KT-33232

// Java's 'since' and 'forRemoval' have no counterpart in 'kotlin.Deprecated', so
// migrating would drop them. The replacement is not suggested once either one is
// passed, explicitly written default values included.

@java.lang.Deprecated(forRemoval = true)
fun withForRemoval() {}

@java.lang.Deprecated(since = "1.0")
fun withSince() {}

@java.lang.Deprecated(since = "1.0", forRemoval = true)
fun withSinceAndForRemoval() {}

@java.lang.Deprecated(forRemoval = false)
fun withExplicitDefaultForRemoval() {}

@java.lang.Deprecated(since = "")
fun withExplicitEmptySince() {}

// Without arguments both annotations make javac report the same '[deprecation]'
// warning, so the replacement is still suggested.
<!DEPRECATED_JAVA_ANNOTATION!>@java.lang.Deprecated<!> fun withoutArguments() {}

/* GENERATED_FIR_TAGS: functionDeclaration, stringLiteral */
