// FILE: a.kt
package a

interface A
interface B : A

private fun validFun() {}
private val validVal = 1

<!CONFLICTING_OVERLOADS!>private fun invalidFun0()<!> {}
private val <!REDECLARATION!>invalidProp0<!> = 1

// NB invalidFun0 and invalidProp0 are conflicting overloads, since the following is an ambiguity:
fun useInvalidFun0() = <!OVERLOAD_RESOLUTION_AMBIGUITY!>invalidFun0<!>()
fun useInvalidProp0() = <!OVERLOAD_RESOLUTION_AMBIGUITY!>invalidProp0<!>

<!CONFLICTING_OVERLOADS!>private fun invalidFun1()<!> {}
<!CONFLICTING_OVERLOADS!>private fun invalidFun1()<!> {}

<!CONFLICTING_OVERLOADS!>private fun invalidFun2()<!> {}
<!CONFLICTING_OVERLOADS!>public fun invalidFun2()<!> {}

<!CONFLICTING_OVERLOADS!>public fun invalidFun3()<!> {}

<!CONFLICTING_OVERLOADS!>private fun invalidFun4()<!> {}
<!CONFLICTING_OVERLOADS!>public fun invalidFun4()<!> {}

public fun validFun2(a: A) = a
public fun validFun2(b: B) = b

// FILE: b.kt
package a

private fun validFun() {}
private val validVal = 1

<!CONFLICTING_OVERLOADS!>private fun invalidFun0()<!> {}

private val <!REDECLARATION!>invalidProp0<!> = 1

<!CONFLICTING_OVERLOADS!>internal fun invalidFun3()<!> {}
<!CONFLICTING_OVERLOADS!>internal fun invalidFun4()<!> {}

// FILE: c.kt
package a

public fun invalidFun0() {}

public val invalidProp0 = 1
