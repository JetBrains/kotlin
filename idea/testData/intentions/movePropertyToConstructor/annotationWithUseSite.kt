annotation class Annotation1(val a: Int = 0)
annotation class Annotation2(val a: Int = 0)
annotation class Annotation3(val a: Int = 0)


class TestClass(@Annotation1(42) @Annotation3(42) initialText: String = "LoremIpsum") {
    private @Annotation1(42) @field:Annotation2(42) val <caret>text = initialText
}