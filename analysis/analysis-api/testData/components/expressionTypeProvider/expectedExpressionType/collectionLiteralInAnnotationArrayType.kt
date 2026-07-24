enum class SomeEnum {
    FOO, BAR
}
annotation class TestAnnotation(
    val requestMapping: Array<SomeEnum>
)
@TestAnnotation(requestMapping = [xy<caret>z])
fun test() {}
