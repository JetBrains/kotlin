// "class com.intellij.codeInspection.SuppressIntentionAction" "false"
// ACTION: Suppress 'REDUNDANT_NULLABLE' for class Child

open class Base<T>
class Child: Base<String?<caret>?>()