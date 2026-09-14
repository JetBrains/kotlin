// LANGUAGE: +FullValueClasses
package pack

value class Inner(val first: String, val second: Int)

value class Ou<caret>ter(val inner: Inner, val nullableInner: Inner?)
