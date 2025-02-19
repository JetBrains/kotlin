/* RootScriptStructureElement */class Foo {/* ClassDeclarationStructureElement */
    @Suppress("") @MustBeDocumented/* DeclarationStructureElement */
}
class Bar {/* ClassDeclarationStructureElement */
    @Suppress("") @MustBeDocumented/* DeclarationStructureElement */
}
class Outer {/* ClassDeclarationStructureElement */
    class Inner {/* ClassDeclarationStructureElement */
        @Suppress("") @MustBeDocumented/* DeclarationStructureElement */
    }
    fun foo() {/* DeclarationStructureElement */
        class Local {
            @Suppress("") @MustBeDocumented
        }
    }
}
