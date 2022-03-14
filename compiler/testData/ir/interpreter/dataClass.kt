@CompileTimeCalculation
data class Person(val name: String, val phone: Int)

@CompileTimeCalculation
fun Person.getAsString(): String {
    val (name, phone) = this
    return "Person name is $name and his phone is $phone"
}

const val a1 = <!EVALUATED: `John`!>Person("John", 123456).name<!>
const val a2 = <!EVALUATED: `John`!>Person("John", 123456).component1()<!>
const val a3 = <!EVALUATED: `123456`!>Person("John", 123456).phone<!>
const val a4 = <!EVALUATED: `123456`!>Person("John", 123456).component2()<!>

const val b1 = <!EVALUATED: `Person(name=Adam, phone=789)`!>Person("John", 789).copy("Adam").toString()<!>
const val b2 = <!EVALUATED: `Person(name=Adam, phone=123)`!>Person("John", 789).copy("Adam", 123).toString()<!>

const val c = <!EVALUATED: `true`!>Person("John", 123456).equals(Person("John", 123456))<!>
const val d = <!EVALUATED: `Person name is John and his phone is 123456`!>Person("John", 123456).getAsString()<!>

@CompileTimeCalculation
data class WithArray(val array: Array<*>?, val intArray: IntArray?)

const val e1 = <!EVALUATED: `WithArray(array=[1, 2.0], intArray=[1, 2, 3])`!>WithArray(arrayOf(1, 2.0), intArrayOf(1, 2, 3)).toString()<!>
const val e2 = <!EVALUATED: `WithArray(array=null, intArray=[1, 2, 3])`!>WithArray(null, intArrayOf(1, 2, 3)).toString()<!>
const val e3 = <!EVALUATED: `WithArray(array=[1, false], intArray=null)`!>WithArray(arrayOf("1", false), null).toString()<!>
const val e4 = <!EVALUATED: `WithArray(array=null, intArray=null)`!>WithArray(null, null).toString()<!>
