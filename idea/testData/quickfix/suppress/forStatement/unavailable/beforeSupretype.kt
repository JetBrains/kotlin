// "Suppress 'REDUNDANT_NULLABLE' for statement " "false"
// ACTION: Remove redundant '?'

open class Base<T>
class Child: Base<String?<caret>?>()