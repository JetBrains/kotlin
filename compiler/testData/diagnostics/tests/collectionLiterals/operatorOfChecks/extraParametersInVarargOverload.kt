// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-80492
// LANGUAGE: +CollectionLiterals
// RENDER_DIAGNOSTIC_ARGUMENTS

class Before {
    companion object {
        operator fun of(before: Int, vararg ints: Int): Before = Before()
    }
}

class After {
    companion object {
        <!INAPPLICABLE_OPERATOR_MODIFIER("must not have vararg parameters other than the last one")!>operator<!> fun of(vararg ints: Int, <!INCONSISTENT_PARAMETER_TYPES_IN_OF_OVERLOADS("Int")!>after: String<!>): After = After()
    }
}

class AlsoVararg {
    companion object {
        <!INAPPLICABLE_OPERATOR_MODIFIER("must not have vararg parameters other than the last one")!>operator<!> fun of(<!MULTIPLE_VARARG_PARAMETERS!>vararg<!> ints: Int, <!INCONSISTENT_PARAMETER_TYPES_IN_OF_OVERLOADS("Int")!><!MULTIPLE_VARARG_PARAMETERS!>vararg<!> alsoVararg: String<!>): AlsoVararg = AlsoVararg()
    }
}

class ReportOnce1 {
    companion object {
        <!INAPPLICABLE_OPERATOR_MODIFIER("must not have vararg parameters other than the last one")!>operator<!> fun of(vararg ints: Int, <!INCONSISTENT_PARAMETER_TYPES_IN_OF_OVERLOADS("Int")!>first: String<!>, <!INCONSISTENT_PARAMETER_TYPES_IN_OF_OVERLOADS("Int")!>second: String<!>): ReportOnce1 = ReportOnce1()
    }
}

class ReportOnce2 {
    companion object {
        <!INAPPLICABLE_OPERATOR_MODIFIER("must not have vararg parameters other than the last one")!>operator<!> fun of(<!INCONSISTENT_PARAMETER_TYPES_IN_OF_OVERLOADS("Int")!>first: String<!>, vararg ints: Int, <!INCONSISTENT_PARAMETER_TYPES_IN_OF_OVERLOADS("Int")!>second: String<!>): ReportOnce2 = ReportOnce2()
    }
}

class DoNotReport {
    companion object {
        operator fun of(<!INCONSISTENT_PARAMETER_TYPES_IN_OF_OVERLOADS("Int")!>first: String<!>, <!INCONSISTENT_PARAMETER_TYPES_IN_OF_OVERLOADS("Int")!>second: String<!>, vararg ints: Int): DoNotReport = DoNotReport()
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, objectDeclaration, operator, vararg */
