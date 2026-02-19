// LANGUAGE: +MultiPlatformProjects, +ContextParameters

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