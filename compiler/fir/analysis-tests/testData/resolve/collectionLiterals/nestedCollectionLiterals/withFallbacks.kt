// LANGUAGE: +CollectionLiterals, +UnnamedLocalVariables
// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB

fun <T> takeSetList(s: Set<Collection<T>>) {
}

fun <T> takeListSet(l: Collection<Set<T>>) {
}

fun tests() {
    takeSetList([[42]])
    takeListSet([[42]])
    takeListSet([[42], ["42"]])
    takeSetList([[42], ["42"]])

    takeSetList([[[42]], [["42"]]])
    takeListSet([[[42], [42u]], [["42"], [<!TOO_MANY_CHARACTERS_IN_CHARACTER_LITERAL!>'42'<!>]]])
}

fun veryNestedList() {
    <!CANNOT_INFER_PARAMETER_TYPE!>[<!CANNOT_INFER_PARAMETER_TYPE!>[<!CANNOT_INFER_PARAMETER_TYPE!>[<!CANNOT_INFER_PARAMETER_TYPE!>[<!CANNOT_INFER_PARAMETER_TYPE!>[<!CANNOT_INFER_PARAMETER_TYPE!>[<!CANNOT_INFER_PARAMETER_TYPE!>[<!CANNOT_INFER_PARAMETER_TYPE!>[<!CANNOT_INFER_PARAMETER_TYPE!>[<!CANNOT_INFER_PARAMETER_TYPE!>[<!CANNOT_INFER_PARAMETER_TYPE!>[<!CANNOT_INFER_PARAMETER_TYPE!>[]<!>]<!>]<!>]<!>]<!>]<!>]<!>]<!>]<!>]<!>]<!>]<!>
    [[[[[[[[[[[[42]]]]]]]]]]]]
    val _: Collection<*> = [[[[[[[[[[[[42]]]]]]]]]]]]
    val _: Set<*> = [[[[[[[[[[[[42]]]]]]]]]]]]
}

/* GENERATED_FIR_TAGS: functionDeclaration, integerLiteral, localProperty, nullableType, propertyDeclaration,
starProjection, stringLiteral, typeParameter, unnamedLocalVariable, unsignedLiteral */
