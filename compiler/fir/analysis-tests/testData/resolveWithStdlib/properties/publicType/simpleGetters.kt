class SomeClass {
    private val items = mutableListOf<String>()
        public get(): List<String>

    fun addItem(item: String) {
        items.add(item)
    }

    private val shmitems = mutableListOf<String>()
        <!EXPOSING_GETTER_WITH_BODY!>protected get(): List<String> = field<!>

    val balalitems = mutableListOf<String>()
        <!GETTER_VISIBILITY_LESS_OR_INCONSISTENT_WITH_PROPERTY_VISIBILITY!>protected<!> get(): List<String> = field

    val rhymetems = mutableListOf<String>()
        get(): <!REDUNDANT_GETTER_TYPE_CHANGE!>List<String><!> = field

    protected val a = 10
        public get(): Number

    protected val c = 11
        <!REDUNDANT_GETTER_VISIBILITY_CHANGE!>public<!> get(): Int

    private val otditems = mutableListOf<String>()
        <!EXPOSING_GETTER_WITH_BODY!>public get(): List<String> {
            println("strelyai, ne otdam")
            return field
        }<!>
}

fun doSomething() {
    val it = SomeClass()
    it.addItem("Test")
    it.items.<!UNRESOLVED_REFERENCE!>add<!>("Fest")

    val itemsSize = it.items.size
    val shmitemsSize = it.<!NONE_APPLICABLE!>shmitems<!>.<!UNRESOLVED_REFERENCE!>size<!>
    val balalitemsSize = it.balalitems.size
    val rhymetemsSize = it.rhymetems.size
}
