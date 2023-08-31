class Foo {/* ClassDeclarationStructureElement */
    @Suppress("") @MustBeDocumented
}
class Bar {/* ClassDeclarationStructureElement */
    @Suppress("") @MustBeDocumented
}
class Outer {/* ClassDeclarationStructureElement */
    class Inner {/* ClassDeclarationStructureElement */
        @Suppress("") @MustBeDocumented
    }
    fun foo() {/* DeclarationStructureElement */
        class Local {
            @Suppress("") @MustBeDocumented
        }
    }
}
