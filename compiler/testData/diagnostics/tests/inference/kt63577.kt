// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// FULL_JDK
// LANGUAGE: -ProperSupportOfInnerClassesInCallableReferenceLHS

fun <ItemType> setupListSpeedSearch() {
    class MatchedItem(val item: ItemType)
    class MatchedItem1<T>(val item: ItemType)
    MatchedItem::class
    MatchedItem::item
    MatchedItem1::class
    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>MatchedItem1<!>::item
    <!CLASS_LITERAL_LHS_NOT_A_CLASS!>MatchedItem1<Int>::class<!>
    MatchedItem1<Int>::item
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, classReference, functionDeclaration, localClass,
nullableType, primaryConstructor, propertyDeclaration, typeParameter */
