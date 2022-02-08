@CompileTimeCalculation
class Person(val name: String, val surname: String) {
    var age: Int
    val wholeName: String

    init {
        wholeName = name + " " + surname
    }

    init {
        age = -1
    }

    constructor(name: String) : this(name, "<NULL>") {}

    constructor() : this("<NOT_GIVEN>") {}

    constructor(name: String, age: Int): this(name) {
        this.age = age
    }
}

const val a1 = Person("Ivan", "Ivanov").<!EVALUATED: `-1`!>age<!>
const val a2 = Person("Ivan", "Ivanov").<!EVALUATED: `Ivan Ivanov`!>wholeName<!>

const val b1 = Person("Ivan").<!EVALUATED: `-1`!>age<!>
const val b2 = Person("Ivan").<!EVALUATED: `Ivan <NULL>`!>wholeName<!>

const val c1 = Person().<!EVALUATED: `-1`!>age<!>
const val c2 = Person().<!EVALUATED: `<NOT_GIVEN> <NULL>`!>wholeName<!>

const val d1 = Person("Ivan", 20).<!EVALUATED: `20`!>age<!>
const val d2 = Person("Ivan", 20).<!EVALUATED: `Ivan <NULL>`!>wholeName<!>

@CompileTimeCalculation
class A {
    val prop: Int
    constructor(arg: Boolean) {
        if (arg) {
            prop = 1
            return
        }
        prop = 2
    }
}

const val e1 = A(true).<!EVALUATED: `1`!>prop<!>
const val e2 = A(false).<!EVALUATED: `2`!>prop<!>
