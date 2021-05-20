// "Create object 'RED'" "false"
// ACTION: Add 'sample =' to argument
// ACTION: Create enum constant 'RED'
// ACTION: Rename reference
// ACTION: Create extension property 'SampleEnum.Companion.RED'
// ACTION: Create member property 'SampleEnum.Companion.RED'
// ERROR: Unresolved reference: RED
enum class SampleEnum {}

fun usage() {
    foo(SampleEnum.RED<caret>)
}

fun foo(sample: SampleEnum) {}