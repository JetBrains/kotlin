// DO_NOT_REQUIRE_NON_PSI_SYMBOL_RESTORATION
annotation class Ann

class C {
    @Ann(value = {
        @Ann
        class <caret>LocalClass
    }
}