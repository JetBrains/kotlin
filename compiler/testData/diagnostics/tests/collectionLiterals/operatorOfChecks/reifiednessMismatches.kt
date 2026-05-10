// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-80492
// LANGUAGE: +CollectionLiterals
// RENDER_DIAGNOSTICS_FULL_TEXT
// SUPPRESS_NO_TYPE_ALIAS_EXPANSION_MODE
//  ^ difference in diagnostic renderer output

class MyList1<T> {
    typealias Self<U> = MyList1<U>
    companion object {
        operator fun <T> of(vararg ts: T): Self<T> = Self()
        operator inline fun <!INCONSISTENT_TYPE_PARAMETERS_IN_OF_OVERLOADS!><reified K><!> of(): Self<K> = Self()
    }
}

class MyList2<T> {
    typealias Self<U> = MyList2<U>
    companion object {
        operator inline fun <reified T> of(vararg ts: T): Self<T> = Self()
        operator inline fun <reified K> of(): Self<K> = Self()
    }
}

class MyList3<T> {
    typealias Self<U> = MyList3<U>
    companion object {
        operator inline fun <reified T> of(vararg ts: T): Self<T> = Self()
        operator fun <!INCONSISTENT_TYPE_PARAMETERS_IN_OF_OVERLOADS!><K><!> of(): Self<K> = Self()
    }
}


/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, inline, nullableType, objectDeclaration,
operator, reified, typeAliasDeclaration, typeAliasDeclarationWithTypeParameter, typeParameter, vararg */
