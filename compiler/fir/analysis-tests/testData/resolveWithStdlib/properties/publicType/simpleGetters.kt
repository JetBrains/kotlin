class SomeClass {
    private val items = mutableListOf<String>()
        public get(): List<String> = field

    fun addItem(item: String) {
        items.add(item)
    }

    private val shmitems = mutableListOf<String>()
        protected get(): List<String> = field

    val balalitems = mutableListOf<String>()
        <!GETTER_VISIBILITY_SMALLER_THAN_PROPERTY_VISIBILITY!>protected<!> get(): List<String> = field

    val rhymetems = mutableListOf<String>()
        get(): <!REDUNDANT_GETTER_TYPE_CHANGE!>List<String><!> = field
}

fun doSomething() {
    val it = SomeClass()
    it.addItem("Test")
    it.items.add("Fest")

    val itemsSize = it.items.size
    val shmitemsSize = it.<!INVISIBLE_REFERENCE!>shmitems<!>.size
    val balalitemsSize = it.balalitems.size
    val rhymetemsSize = it.rhymetems.size
}
