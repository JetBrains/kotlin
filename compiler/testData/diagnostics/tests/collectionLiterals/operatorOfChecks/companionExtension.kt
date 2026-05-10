// LANGUAGE: +CompanionBlocksAndExtensions +CollectionLiterals
// RUN_PIPELINE_TILL: FRONTEND

class MyList

companion <!INAPPLICABLE_OPERATOR_MODIFIER, INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun MyList.of(vararg x: String): MyList = MyList()

class MyGenericList<T>

companion <!INAPPLICABLE_OPERATOR_MODIFIER, INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun <T> MyGenericList.of(vararg t: T): MyGenericList<T> = MyGenericList<T>()

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, nullableType, operator,
typeParameter, vararg */
