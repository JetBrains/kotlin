// FILE: a.kt
package a

interface A
interface B : A

private fun validFun() {}

private val validProp = 1

<!CONFLICTING_OVERLOADS!>private fun invalidFun1()<!> {}
<!CONFLICTING_OVERLOADS!>private fun invalidFun1()<!> {}

<!CONFLICTING_OVERLOADS!>private fun invalidFun2()<!> {}
<!CONFLICTING_OVERLOADS!>public fun invalidFun2()<!> {}

<!CONFLICTING_OVERLOADS!>public fun invalidFun3()<!> {}

<!CONFLICTING_OVERLOADS!>private fun invalidFun4()<!> {}
<!CONFLICTING_OVERLOADS, CONFLICTING_OVERLOADS!>public fun invalidFun4()<!> {}

public fun validFun2(a: A) = a
public fun validFun2(b: B) = b

// FILE: b.kt
package a

private fun validFun() {}

private val validProp = 1

<!CONFLICTING_OVERLOADS!>internal fun invalidFun3()<!> {}
<!CONFLICTING_OVERLOADS!>internal fun invalidFun4()<!> {}

// FILE: c.kt
package a

public fun validFun() {}

public val validProp = 1