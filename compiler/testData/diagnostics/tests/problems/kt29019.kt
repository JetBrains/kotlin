// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-29019

// KT-29019: Code with generic types compiles forever
interface InterfaceA<TA, TB, TC, TD, TE, TF,
        out TG : InterfaceD<TE, TC, TD, TF, TH>,
        out TH : InterfaceE<TF, TC, TD, TE, TG>>

interface InterfaceB<TA, TB, TC, TD, TE, TF,
        out TG : InterfaceD<TE, TC, TD, TF, TH>,
        out TH : InterfaceE<TF, TC, TD, TE, TG>> :
    InterfaceA<TA, TB, TC, TD, TE, TF, TG, TH>

interface InterfaceC<TA, TB, TC, TD, TE, TF,
        out TG : InterfaceD<TE, TC, TD, TF, TH>,
        out TH : InterfaceE<TF, TC, TD, TE, TG>> :
    InterfaceA<TA, TB, TC, TD, TE, TF, TG, TH>

interface InterfaceD<TA, TB, TC, TD,
        out TE : InterfaceE<TD, TB, TC, TA, InterfaceD<TA, TB, TC, TD, TE>>> :
    InterfaceC<TB, TA, TB, TC, TA, TD, InterfaceD<TA, TB, TC, TD, TE>, TE>

interface InterfaceE<TA, TB, TC, TD,
        out TE : InterfaceD<TD, TB, TC, TA, InterfaceE<TA, TB, TC, TD, TE>>> :
    InterfaceB<TA, TC, TB, TC, TD, TA, TE, InterfaceE<TA, TB, TC, TD, TE>>

class BugClass<TA, TB, TC, TD, TE, TF,
        TG : InterfaceD<TE, TC, TD, TF, TH>,
        TH : InterfaceE<TF, TC, TD, TE, TG>,
        out TI : InterfaceA<TA, TB, TC, TD, TE, TF, TG, TH>,
        TJ>
    (val a: TI)

class TestClass<TA, TB, TC,
        out TD : InterfaceE<TC, Unit, TB, TA, InterfaceD<TA, Unit, TB, TC, TD>>>
    (val a: InterfaceB<TA, *, Unit, TB, TA, TC, InterfaceD<TA, Unit, TB, TC, TD>, TD>) :
    InterfaceD<TA, Unit, TB, TC, TD>
{
    fun test()
    {
        <!CANNOT_INFER_PARAMETER_TYPE!>BugClass<!>(a)
    }
}

/* GENERATED_FIR_TAGS: capturedType, classDeclaration, functionDeclaration, interfaceDeclaration, nullableType, out,
primaryConstructor, propertyDeclaration, starProjection, typeConstraint, typeParameter */
