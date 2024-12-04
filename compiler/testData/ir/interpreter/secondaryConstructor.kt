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

const val a1 = <!EVALUATED: `-1`!>Person("Ivan", "Ivanov").age<!>
const val a2 = <!EVALUATED: `Ivan Ivanov`!>Person("Ivan", "Ivanov").wholeName<!>

const val b1 = <!EVALUATED: `-1`!>Person("Ivan").age<!>
const val b2 = <!EVALUATED: `Ivan <NULL>`!>Person("Ivan").wholeName<!>

const val c1 = <!EVALUATED: `-1`!>Person().age<!>
const val c2 = <!EVALUATED: `<NOT_GIVEN> <NULL>`!>Person().wholeName<!>

const val d1 = <!EVALUATED: `20`!>Person("Ivan", 20).age<!>
const val d2 = <!EVALUATED: `Ivan <NULL>`!>Person("Ivan", 20).wholeName<!>

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

const val e1 = <!EVALUATED: `1`!>A(true).prop<!>
const val e2 = <!EVALUATED: `2`!>A(false).prop<!>
