/* RootScriptStructureElement */class Foo {/* ClassDeclarationStructureElement */
    @Suppress(""
    fun foo() {}/* DeclarationStructureElement */
}
class Bar {/* ClassDeclarationStructureElement */
    @Suppress(
    fun foo() {}/* DeclarationStructureElement */
}
class Outer {/* ClassDeclarationStructureElement */
    class Inner {/* ClassDeclarationStructureElement */
        @Suppress(""
        fun foo() {}/* DeclarationStructureElement */
    }
    fun foo() {/* DeclarationStructureElement */
        class Local {
            @Suppress(""
            fun foo() {}
        }
    }
}
