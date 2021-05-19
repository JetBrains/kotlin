@CompileTimeCalculation
fun <T> getArray(array: Array<T>) = array

const val a1 = <!EVALUATED: `true`!>getArray(arrayOf(1, 2.0, "3")) as? Array<Int> == null<!>
const val a2 = <!EVALUATED: `false`!>getArray(arrayOf(1, 2, 3)) as? Array<Int> == null<!>
const val a3 = <!EVALUATED: `true`!>getArray(arrayOf(1, 2, 3)) as? Array<Double> == null<!>
const val a4 = <!EVALUATED: `false`!>getArray(arrayOf(1, 2, 3)) as? Array<Number> == null<!>

const val b1 = <!EVALUATED: `true`!>arrayOf(arrayOf(1, 2, 3)) as? Array<Array<String>> == null<!>
const val b2 = <!EVALUATED: `false`!>arrayOf(arrayOf(1, 2, 3)) as? Array<Array<Int>> == null<!>
const val b3 = <!EVALUATED: `false`!>arrayOf(arrayOf(1, 2, 3)) as? Array<Array<Number>> == null<!>

const val c1 = <!EVALUATED: `false`!>arrayOf(arrayOf(1, 2, 3), arrayOf("1", "2", "3"))[0] as? Array<Int> == null<!>
const val c2 = <!EVALUATED: `false`!>arrayOf(arrayOf(1, 2, 3), arrayOf("1", "2", "3"))[1] as? Array<String> == null<!>

@CompileTimeCalculation
fun <T, E> combineArrays(array1: Array<T>, array2: Array<E>) = arrayOf(array1, array2)

const val d1 = <!EVALUATED: `true`!>combineArrays(arrayOf(1, 2, 3), arrayOf(1, 2, 3)) as? Array<Array<Int>> == null<!>
const val d2 = <!EVALUATED: `true`!>combineArrays(arrayOf(1, 2, 3), arrayOf(1, 2, 3)) as? Array<Array<Number>> == null<!>
const val d3 = <!EVALUATED: `false`!>combineArrays(arrayOf(1, 2, 3), arrayOf(1, 2, 3)) as? Array<Array<Any>> == null<!>
const val d4 = <!EVALUATED: `false`!>combineArrays(arrayOf(1, 2, 3), arrayOf(1, 2, 3)) as? Array<Array<*>> == null<!>
const val d5 = <!EVALUATED: `false`!>combineArrays(arrayOf(1, 2, 3), arrayOf("1", "2", "3"))[0] as? Array<Int> == null<!>
const val d6 = <!EVALUATED: `true`!>combineArrays(arrayOf(1, 2, 3), arrayOf("1", "2", "3"))[1] as? Array<Int> == null<!>
const val d7 = <!EVALUATED: `false`!>combineArrays(arrayOf(1, 2, 3), arrayOf("1", "2", "3"))[1] as? Array<String> == null<!>

@CompileTimeCalculation
fun <T> echo(array: T) = array
const val e1 = <!EVALUATED: `false`!>echo<Any>(arrayOf(1, 2, 3)) as? Array<Int> == null<!>
const val e2 = <!EVALUATED: `false`!>echo<Any>(arrayOf(arrayOf(1, 2, 3))) as? Array<Array<Int>> == null<!>
const val e3 = <!EVALUATED: `true`!>echo<Any>(arrayOf(echo<Any>(1), echo<Any>(2), echo<Any>(3))) as? Array<Int> == null<!>
