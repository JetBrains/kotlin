package one.two

import one.two.TopLevelClass.InnerClass as InnerAlias

class TopLevelClass {
    class NestedClass
    inner class InnerClass
}

fun one.two.TopLevelClass.foo(nestedClass: TopLevelClass.NestedClass) {
    val innerClass: InnerAlias = InnerClass()
}
