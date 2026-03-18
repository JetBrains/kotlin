// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-83770

interface InterfaceWithVarNumber { var y: Number }
interface InterfaceWithValNumber { val y: Number }

open class ClassWithValAnyAndField {
    val y: Any
        field = 20
}

<!VAR_IMPLEMENTED_BY_INHERITED_VAL_ERROR, VAR_TYPE_MISMATCH_ON_INHERITANCE!>class TestA<!> : ClassWithValAnyAndField(), InterfaceWithVarNumber
<!PROPERTY_TYPE_MISMATCH_ON_INHERITANCE!>class TestB<!> : ClassWithValAnyAndField(), InterfaceWithValNumber

interface InterfaceWithVarAny { var y: Any }
interface InterfaceWithValAny { val y: Any }

open class ClassWithValNumberAndField {
    val y: Number
        field = 20
}

<!VAR_IMPLEMENTED_BY_INHERITED_VAL_ERROR, VAR_TYPE_MISMATCH_ON_INHERITANCE!>class TestC<!> : ClassWithValNumberAndField(), InterfaceWithVarAny
class TestD : ClassWithValNumberAndField(), InterfaceWithValAny

/* GENERATED_FIR_TAGS: classDeclaration, explicitBackingField, integerLiteral, interfaceDeclaration, propertyDeclaration */
