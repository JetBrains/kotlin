// RUN_PIPELINE_TILL: BACKEND
// SKIP_FIR_DUMP

class TopLevelClass1
class TopLevelClass2
class TopLevelClass3

open class AliasHolder {
    typealias TopLevelClassTA = TopLevelClass1

    class Nested
    typealias NestedTA = Nested

    inner class Inner
    typealias InnerTA = Inner

    private val testTopLevel: TopLevelClassTA = TopLevelClass1()
    private val nestNested: NestedTA = Nested()
    private val testInner: InnerTA = AliasHolder().Inner()
}

open class OverridingSubAliasHolder : AliasHolder() {
    typealias TopLevelClassTA = TopLevelClass2

    class Nested2
    typealias NestedTA = Nested2

    inner class Inner2
    typealias InnerTA = Inner2

    private val testTopLevel: TopLevelClassTA = TopLevelClass2()
    private val nestNested: NestedTA = Nested2()
    private val testInner: InnerTA = OverridingSubAliasHolder().Inner2()
}

open class SubAliasHolder : AliasHolder()

open class SubSubAliasHolder : SubAliasHolder() {
    typealias TopLevelClassTA = TopLevelClass3

    class Nested3
    typealias NestedTA = Nested3

    inner class Inner3
    typealias InnerTA = Inner3

    private val testTopLevel: TopLevelClassTA = TopLevelClass3()
    private val nestNested: NestedTA = Nested3()
    private val testInner: InnerTA = Inner3()
}
