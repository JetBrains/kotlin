/* RootStructureElement */@Target(AnnotationTarget.TYPE, AnnotationTarget.VALUE_PARAMETER)
annotation class Anno(val position: String)/* DeclarationStructureElement *//* ClassDeclarationStructureElement */

class Foo {/* ClassDeclarationStructureElement */
    context(@Anno("param") parameter1: @Anno("1" + "2") String, parameter2: List<@Anno("str") Int>)/* DeclarationStructureElement */
}


fun foo() {/* DeclarationStructureElement */
    class Foo {
        context(@Anno("param") parameter1: @Anno("1" + "2") String, parameter2: List<@Anno("str") Int>)
    }
}

context(@Anno("param") parameter1 : @Anno("1" + "2") String, parameter2: List<@Anno("str") Int>)/* DeclarationStructureElement */
