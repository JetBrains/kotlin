// LANGUAGE: +ExpectedTypeGuidedResolution
package test

enum class Sample {
    FIRST, SECOND, THIRD;
}

fun sample(s: Sample): Int = 1

fun foo() = sample(.FIRST)