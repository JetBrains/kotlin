// FILE: annotation.kt

package test

@Target(AnnotationTarget.FILE) annotation class special

annotation class common

// FILE: other.kt

@file:special

package test

@special class Incorrect

// FILE: another.kt

@file:common

package test

@common class Correct
