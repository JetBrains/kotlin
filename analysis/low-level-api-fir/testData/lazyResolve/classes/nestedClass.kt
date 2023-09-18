open class TopLevelClass
open class AnotherTopLevelClass : TopLevelClass()
class OuterClass : AnotherTopLevelClass() {
    class Nested<caret>Class
}
