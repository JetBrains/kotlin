// WITH_RUNTIME

open class A {
    private open val tags = listOf("a", "b")
        public get(): Collection<String>
}

open class B : A() {
    protected override val tags = mutableListOf("#test", "#fest", "#rest")
        public get(): List<String>

    init {
        // NPE for overrides
        // tags.addAll(listOf("#test", "#fest", "#rest"))
        // names...
    }

    fun registerTag(tag: String) {
        tags.add(tag)
    }

    open val names = mutableListOf("A", "B", "C")

    fun registerName(name: String) {
        names.add(name)
    }
}

class C : B() {
    public override val tags = mutableListOf("#ok", "#fine", "#right", "#agreed", "#c_tag")

    override val names = mutableListOf("D", "E", "F", "G")
}

fun box(): String {
    val a = A()

    if (a.tags.firstOrNull() != "a") {
        return "fail: 1 => ${a.tags}"
    }

    val b = B()

    if (b.tags.lastOrNull() != "#rest") {
        return "fail: 2 => ${b.tags}"
    }

    b.registerTag("#new_tag")

    if ("#new_tag" !in b.tags) {
        return "fail: 3 => ${b.tags}"
    }

    val c = C()

    if ("#c_tag" !in c.tags) {
        return "fail: 4 => ${c.tags}"
    }

    c.registerTag("#new_tag")
    c.tags.add("#raw_access")

    if (c.tags.size != 7) {
        return "fail: 5 => ${c.tags}"
    }

    c.registerName("H")

    if (c.names.size != 5) {
        return "fail: 6 => ${c.names}"
    }

    return "OK"
}