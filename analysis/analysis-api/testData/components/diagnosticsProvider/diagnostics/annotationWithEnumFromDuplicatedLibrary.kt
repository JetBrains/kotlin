// ALLOW_KOTLIN_PACKAGE
// DISABLE_SEALED_INHERITOR_CALCULATOR
// MODULE: lib1
// FILE: anno.kt
package kotlin.annotation

public enum class AnnotationTarget {
    CLASS;
}

// MODULE: dep(lib1)
// FILE: annotation.kt
package my.pack

import kotlin.annotation.AnnotationTarget

@Target(AnnotationTarget.CLASS)
annotation class Deprecated

// MODULE: lib2
// FILE: anno.kt
package kotlin.annotation

public enum class AnnotationTarget {
    CLASS;
}

// MODULE: main(lib2, dep)
// FILE: usage.kt
package usage

import my.pack.Deprecated

@Deprecated
class Usage