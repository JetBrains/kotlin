// "Create object 'RED'" "false"
// ACTION: Add 'sample =' to argument
// ACTION: Create enum constant 'RED'
// ACTION: Rename reference
// ERROR: Unresolved reference: RED
enum class SampleEnum {}

fun usage() {
    foo(SampleEnum.RED<caret>)
}

fun foo(sample: SampleEnum) {}