// WITH_STDLIB
// FULL_JDK

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
