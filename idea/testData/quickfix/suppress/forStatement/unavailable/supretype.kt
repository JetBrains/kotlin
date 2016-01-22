// "class com.intellij.codeInspection.SuppressIntentionAction" "false"
// ACTION: Suppress 'REDUNDANT_NULLABLE' for class Child
// ACTION: Suppress 'REDUNDANT_NULLABLE' for file supretype.kt

open class Base<T>
class Child: Base<String?<caret>?>()