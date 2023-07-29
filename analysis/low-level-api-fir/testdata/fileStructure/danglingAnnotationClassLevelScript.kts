/* RootScriptStructureElement */class Foo {/* NonReanalyzableClassDeclarationStructureElement */
    @Suppress("") @MustBeDocumented
}
class Bar {/* NonReanalyzableClassDeclarationStructureElement */
    @Suppress("") @MustBeDocumented
}
class Outer {/* NonReanalyzableClassDeclarationStructureElement */
    class Inner {/* NonReanalyzableClassDeclarationStructureElement */
        @Suppress("") @MustBeDocumented
    }
    fun foo() {/* ReanalyzableFunctionStructureElement */
        class Local {
            @Suppress("") @MustBeDocumented
        }
    }
}
