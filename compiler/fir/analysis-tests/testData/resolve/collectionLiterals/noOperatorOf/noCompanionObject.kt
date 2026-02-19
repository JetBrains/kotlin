// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-79330
// LANGUAGE: +CollectionLiterals

class MyList

class MyGenericList<T> {
    object NonCompanion {
        <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun of(vararg strs: String): MyGenericList<String> = MyGenericList()
    }
}

fun acceptList(l: MyList) { }
fun <T2> acceptGenericList(l: MyGenericList<T2>) { }

fun test() {
    acceptList(<!UNRESOLVED_REFERENCE!>["1", "2", "3"]<!>)
    <!CANNOT_INFER_PARAMETER_TYPE!>acceptGenericList<!>(<!UNRESOLVED_REFERENCE!>["1", "2", "3"]<!>)
    acceptGenericList<String>(<!UNRESOLVED_REFERENCE!>["1", "2", "3"]<!>)
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, nestedClass, nullableType, objectDeclaration, operator,
stringLiteral, typeParameter, vararg */
