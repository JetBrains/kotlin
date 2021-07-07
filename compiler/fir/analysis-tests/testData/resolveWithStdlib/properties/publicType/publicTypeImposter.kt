open class SomeType1 {
    open protected var items = mutableListOf<String>()
        public get(): List<String>
}

class Imposter1 : SomeType1() {
    private val realItems = mutableListOf<String>()

    <!INCOMPLETE_PROPERTY_OVERRIDE!>override var items = realItems<!>
}

class Imposter2 : SomeType1() {
    private val realItems = mutableListOf<String>()

    override var items = realItems
        <!REDUNDANT_GETTER_VISIBILITY_CHANGE!>public<!> get(): MutableList<String>
}

fun testSomeType1() {
    val SomeType1 = SomeType1()
    SomeType1.items.<!UNRESOLVED_REFERENCE!>add<!>("Test")

    val imposter1 = Imposter1()
    imposter1.<!INVISIBLE_REFERENCE!>items<!>.add("Rest")

    val imposter2 = Imposter2()
    imposter2.items.add("Rest")
}

open class SomeType2 {
    open protected var items = mutableListOf<String>()
}

class Imposter3 : SomeType2() {
    private val realItems = mutableListOf<String>()

    override var items = realItems
}

class Imposter4 : SomeType2() {
    private val realItems = mutableListOf<String>()

    override var items = realItems
        <!REDUNDANT_GETTER_VISIBILITY_CHANGE!>public<!> get(): MutableList<String>
}

class Imposter5 : SomeType2() {
    <!MUST_BE_INITIALIZED!>override var items<!>
        get() = mutableListOf("Test")
}

class Imposter6 : SomeType2() {
    <!MUST_BE_INITIALIZED_OR_BE_ABSTRACT!>override var items<!>
        public get(): MutableList<String>
}

fun testSomeType2() {
    val SomeType2 = SomeType2()
    SomeType2.<!INVISIBLE_REFERENCE!>items<!>.add("Test")

    val imposter3 = Imposter3()
    imposter3.<!INVISIBLE_REFERENCE!>items<!>.add("Rest")

    val imposter4 = Imposter4()
    imposter4.items.add("Rest")

    val imposter5 = Imposter5()
    imposter5.<!INVISIBLE_REFERENCE!>items<!>.add("Rest")

    val imposter6 = Imposter6()
    imposter6.items.add("Rest")
}
