// "Add type 'String' to parameter 'value'" "true"
// LANGUAGE_VERSION: 1.2

annotation class CollectionDefault(vararg val value = ["alpha", "beta"]<caret>)