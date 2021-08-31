@CompileTimeCalculation
data class Person(val name: String, val phone: Int)

@CompileTimeCalculation
fun Person.getAsString(): String {
    val (name, phone) = this
    return "Person name is $name and his phone is $phone"
}

const val a1 = Person("John", 123456).<!EVALUATED: `John`!>name<!>
const val a2 = Person("John", 123456).<!EVALUATED: `John`!>component1()<!>
const val a3 = Person("John", 123456).<!EVALUATED: `123456`!>phone<!>
const val a4 = Person("John", 123456).<!EVALUATED: `123456`!>component2()<!>

const val b1 = Person("John", 789).copy("Adam").<!EVALUATED: `Person(name=Adam, phone=789)`!>toString()<!>
const val b2 = Person("John", 789).copy("Adam", 123).<!EVALUATED: `Person(name=Adam, phone=123)`!>toString()<!>

const val c = Person("John", 123456).<!EVALUATED: `true`!>equals(Person("John", 123456))<!>
const val d = Person("John", 123456).<!EVALUATED: `Person name is John and his phone is 123456`!>getAsString()<!>

@CompileTimeCalculation
data class WithArray(val array: Array<*>?, val intArray: IntArray?)

const val e1 = WithArray(arrayOf(1, 2.0), intArrayOf(1, 2, 3)).<!EVALUATED: `WithArray(array=[1, 2.0], intArray=[1, 2, 3])`!>toString()<!>
const val e2 = WithArray(null, intArrayOf(1, 2, 3)).<!EVALUATED: `WithArray(array=null, intArray=[1, 2, 3])`!>toString()<!>
const val e3 = WithArray(arrayOf("1", false), null).<!EVALUATED: `WithArray(array=[1, false], intArray=null)`!>toString()<!>
const val e4 = WithArray(null, null).<!EVALUATED: `WithArray(array=null, intArray=null)`!>toString()<!>
