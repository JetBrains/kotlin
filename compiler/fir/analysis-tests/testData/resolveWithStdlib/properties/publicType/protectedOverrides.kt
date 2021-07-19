open class A {
    private open var items = mutableListOf<String>()
        protected get(): List<String>

    fun put(item: String) {
        items.add(item)
    }

    fun use(other: MutableList<String>) {
        items = other
    }

    override fun toString() = "Items: $items"
}

class B : A() {
    private val realItems = listOf<String>()

    <!INCOMPLETE_PROPERTY_OVERRIDE!>override var <!VAR_TYPE_MISMATCH_ON_OVERRIDE!>items<!> = realItems<!>

    override fun toString() = "Items: $items, Real Items: $realItems"
}

class C: A() {
    private val itemsSource = listOf("A", "B", "C")
    private var itemsSink = listOf<String>()

    <!INCOMPLETE_PROPERTY_OVERRIDE!>override var <!VAR_TYPE_MISMATCH_ON_OVERRIDE!>items<!>
        get() = itemsSource
        set(value) {
            itemsSink = value
        }<!>

    override fun toString() = "Items: $items, Source: $itemsSource, Sink: $itemsSink"
}

class D : A() {
    override fun toString() = "Items: $items"

    fun add(item: String) {
        items.<!UNRESOLVED_REFERENCE!>add<!>(item)
    }
}

class E : A() {
    private val realItems = listOf<String>()

    protected override var <!VAR_TYPE_MISMATCH_ON_OVERRIDE!>items<!> = realItems

    override fun toString() = "Items: $items, Real Items: $realItems"

    fun add(item: String) {
        items.<!UNRESOLVED_REFERENCE!>add<!>(item)
    }
}

class F : A() {
    private val itemsSource = listOf("A", "B", "C")
    private var itemsSink = listOf<String>()

    protected override var <!VAR_TYPE_MISMATCH_ON_OVERRIDE!>items<!>
        get() = itemsSource
        set(value) {
            itemsSink = value
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
