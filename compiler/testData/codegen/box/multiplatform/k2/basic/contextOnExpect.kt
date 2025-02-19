// LANGUAGE: +MultiPlatformProjects, +ContextParameters
// IGNORE_NATIVE: compatibilityTestMode=BACKWARD
// ^^^ Compiler v2.1.10 does not know this language feature

// MODULE: common
// FILE: common.kt

package test

expect class Sample

context(s: Sample)
fun contextFunction(): String = "O"

// MODULE: platform()()(common)
// FILE: platform.kt

package test

actual class Sample

context(s: Sample)
fun platformSpecificContextFunction(): String = "K"


fun box(): String {
    with(Sample()){
        return contextFunction() + platformSpecificContextFunction()
    }
}