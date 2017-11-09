import kotlin.collections.ArrayList as KotlinArrayList

fun foo(): KotAr<caret>

// EXIST: { lookupString: "KotlinArrayList", itemText: "KotlinArrayList", tailText: "<E> (kotlin.collections.ArrayList)", typeText: "ArrayList<E>" }
