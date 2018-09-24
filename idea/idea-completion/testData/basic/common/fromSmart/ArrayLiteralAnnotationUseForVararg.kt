annotation class Bar(vararg val a: String)

@Bar(*<caret>) fun bar() {}

// EXIST: "[]"
// LANGUAGE_VERSION: 1.2