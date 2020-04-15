package test

import test.TopLevelClass.NestedClass.*
import test.TopLevelEnum.NestedEnum.*
import test.TopLevelEnum.*

private class TopLevelClass {
    private class NestedClass {
        class A1
        object A2
    }

    fun test() {
        A1()
        A2
    }
}

<!EXPOSED_FUNCTION_RETURN_TYPE, EXPOSED_FUNCTION_RETURN_TYPE!>private enum class TopLevelEnum(private val e: NestedEnum) {
    E1(NestedEntry);

    private enum class NestedEnum {
        NestedEntry;
    }
}

fun testAccess() {
    E1
    NestedEntry
    A1()
    A2
}