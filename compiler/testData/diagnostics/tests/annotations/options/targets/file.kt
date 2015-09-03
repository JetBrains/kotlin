// FILE: annotation.kt

package test

target(AnnotationTarget.FILE) annotation class special

annotation class common

// FILE: other.kt

@file:special

package test

<!WRONG_ANNOTATION_TARGET!>special<!> class Incorrect

// FILE: another.kt

<!WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@file:common<!>

package test

common class Correct