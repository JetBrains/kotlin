val v = 1

fun foo([<caret>) { }

// EXIST: inlineOptions
// ABSENT: String
// ABSENT: v
