annotation class Annotation1(val a: Int = 0)
annotation class Annotation2(val a: Int = 0)
annotation class Annotation3(val a: Int = 0)


class TestClass(private @param:Annotation1(42) @property:Annotation1(42) @field:Annotation2(42) @Annotation3(42) val <caret>text: String = "LoremIpsum")