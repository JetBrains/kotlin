interface I : () -> Unit

fun xfoo(p: () -> Unit){}

val global: () -> Unit = { }

fun X.test(p1: () -> Unit, p2: () -> String, p3: I) {
    val local: () -> Unit = { }
    xfoo<caret>
}

interface X {
    public val publicVal: () -> Unit
    protected val protectedVal: () -> Unit
}

val X.extension: () -> Unit
    get() = {}

val String.wrongExtension: () -> Unit
    get() = {}

// EXIST: { itemText: "xfoo", tailText: " {...} (p: () -> Unit) (<root>)", typeText:"Unit" }
// EXIST: { itemText: "xfoo", tailText: "(p1) (<root>)", typeText: "Unit" }
// EXIST: { itemText: "xfoo", tailText: "(p3) (<root>)", typeText: "Unit" }
// EXIST: { itemText: "xfoo", tailText: "(local) (<root>)", typeText: "Unit" }
// EXIST: { itemText: "xfoo", tailText: "(global) (<root>)", typeText: "Unit" }
// EXIST: { itemText: "xfoo", tailText: "(publicVal) (<root>)", typeText: "Unit" }

// excluded by performance reasons:
// ABSENT: { itemText: "xfoo", tailText: "(extension) (<root>)", typeText: "Unit" }

// NOTHING_ELSE
