// SKIP_WHEN_OUT_OF_CONTENT_ROOT
// DISABLE_SEALED_INHERITOR_CALCULATOR
// MODULE: lib1
// FILE: dependency.kt
package usage

fun implicitType() = 1

// MODULE: dep(lib1)
// FILE: annotation.kt
package usage

fun implicitTypeFromAnotherModule() = implicitType()

// MODULE: lib2
// FILE: dependency.kt
package usage

fun implicitType() = "str"

// MODULE: main(lib2, dep)
// FILE: usage.kt
package usage

fun usa<caret>ge() = run {
    implicitType()
    implicitTypeFromAnotherModule()
}