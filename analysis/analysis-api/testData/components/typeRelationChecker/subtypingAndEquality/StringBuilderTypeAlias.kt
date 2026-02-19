package test

val ja<caret_type1>vaStringBuilder: java.lang.StringBuilder = StringBuilder()

val ko<caret_type2>tlinStringBuilder: kotlin.text.StringBuilder = StringBuilder()

// ARE_EQUAL: true
// ARE_EQUAL_LENIENT: true
// IS_SUBTYPE: true
// IS_SUBTYPE_LENIENT: true

// SUPERCLASS_ID: kotlin/text/StringBuilder
// IS_CLASS_SUBTYPE: true
// IS_CLASS_SUBTYPE_LENIENT: true
