package test

import test.TopLevelClass.NestedClass
import test.TopLevelClass.NestedClass.InNested
import test.TopLevelEnum.NestedEnum.NestedEntry
import test.TopLevelObject.inObject
import test.TopLevelEnum.E1

private class TopLevelClass {
    private class NestedClass {
        class InNested
    }

    fun test() {
        InNested()
    }
}

private enum class TopLevelEnum(private val e: NestedEnum) {
    E1(NestedEntry);

    private enum class NestedEnum {
        NestedEntry;
    }
}

private object TopLevelObject {
    fun inObject() {}
}

fun testAccess() {
    <!INVISIBLE_MEMBER!>NestedClass<!>()
    E1
    <!INVISIBLE_MEMBER!>InNested<!>()
    <!INVISIBLE_MEMBER!>NestedEntry<!>
    inObject()
}