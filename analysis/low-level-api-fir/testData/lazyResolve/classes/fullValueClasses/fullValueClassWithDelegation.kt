// LANGUAGE: +FullValueClasses
package pack

value class Wrap<caret>per(val value: Int, val name: String) : Comparable<Int> by value
