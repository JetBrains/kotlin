// "Create enum constant 'RED'" "true"
enum class SampleEnum {}

fun usage(sample: SampleEnum) {
    if (sample == SampleEnum.RED<caret>) {
    }
}