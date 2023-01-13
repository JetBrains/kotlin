class Foo {/* NonReanalyzableDeclarationStructureElement */
    @Suppress("") @MustBeDocumented
}
class Bar {/* NonReanalyzableDeclarationStructureElement */
    @Suppress("") @MustBeDocumented
}
class Outer {/* NonReanalyzableDeclarationStructureElement */
    class Inner {/* NonReanalyzableDeclarationStructureElement */
        @Suppress("") @MustBeDocumented
    }
    fun foo() {/* ReanalyzableFunctionStructureElement */
        class Local {
            @Suppress("") @MustBeDocumented
        }
    }
}
