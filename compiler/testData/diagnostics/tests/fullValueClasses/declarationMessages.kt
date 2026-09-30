// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +FullValueClasses
// RENDER_DIAGNOSTICS_FULL_TEXT
// WITH_STDLIB

@JvmInline
<!VALUE_CLASS_NOT_FINAL!>open<!> value class OpenInline(val x: Int)

value class VarargProperty(<!VALUE_CLASS_CONSTRUCTOR_NOT_FINAL_READ_ONLY_PARAMETER!>vararg val xs: Int<!>)

value class VarProperty(<!VALUE_CLASS_CONSTRUCTOR_NOT_FINAL_READ_ONLY_PARAMETER!>var x: Int<!>)

@JvmInline
value class UnitProperty(val u: <!VALUE_CLASS_HAS_INAPPLICABLE_PARAMETER_TYPE!>Unit<!>)

class Outer {
    inner <!VALUE_CLASS_NOT_TOP_LEVEL!>value<!> class Inner(val x: Int)
}

/* GENERATED_FIR_TAGS: classDeclaration, inner, primaryConstructor, propertyDeclaration, value, vararg */
