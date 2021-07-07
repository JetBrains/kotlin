open class A {
    open protected var items = mutableListOf<String>()
        public get(): List<String>

    fun put(item: String) {
        items.add(item)
    }

    fun use(other: MutableList<String>) {
        items = other
    }

    override fun toString() = "Items: $items"
}

class B : A() {
    private val realItems = mutableListOf<String>()

    <!INCOMPLETE_PROPERTY_OVERRIDE!>override var items = realItems<!>

    override fun toString() = "Items: $items, Real Items: $realItems"
}

class C: A() {
    private val itemsSource = mutableListOf("A", "B", "C")
    private var itemsSink = mutableListOf<String>()

    <!INCOMPLETE_PROPERTY_OVERRIDE!>override var items
        get() = itemsSource
        set(value) {
            itemsSink = value
        }<!>

    override fun toString() = "Items: $items, Source: $itemsSource, Sink: $itemsSink"
}

class D : A() {
    override fun toString() = "Items: $items"

    fun add(item: String) {
        items.add(item)
    }
}

class E : A() {
    private val realItems = listOf<String>()

    <!INCOMPLETE_PROPERTY_OVERRIDE!>override var <!VAR_TYPE_MISMATCH_ON_OVERRIDE!>items<!> = realItems<!>

    override fun toString() = "Items: $items, Real Items: $realItems"

    fun add(item: String) {
        items.<!UNRESOLVED_REFERENCE!>add<!>(item)
    }
}

class F : A() {
    private val realItems = mutableListOf<String>()

    <!INCOMPLETE_PROPERTY_OVERRIDE!>override var items = realItems<!>

    override fun toString() = "Items: $items, Real Items: $realItems"

    fun add(item: String) {
        items.add(item)
    }
}

fun main() {
    val b = B()
    b.put("Test")
    println(b)
    b.use(mutableListOf("X", "Y", "Z", "W"))
    println(b)

    val c = C()
    c.put("Rest")
    println(c)
    c.use(mutableListOf("X", "Y", "Z", "W"))
    println(c)
}
