/* NonReanalyzableNonClassDeclarationStructureElement */class Foo {
    @Suppress("") @MustBeDocumented
}
class Bar {
    @Suppress("") @MustBeDocumented
}
class Outer {
    class Inner {
        @Suppress("") @MustBeDocumented
    }
    fun foo() {
        class Local {
            @Suppress("") @MustBeDocumented
        }
    }
}
