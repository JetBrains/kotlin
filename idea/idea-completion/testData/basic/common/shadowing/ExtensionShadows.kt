class Shadow {
    fun shade() {}
}

fun <X> Shadow.shade() {}

fun context() {
    Shadow().sha<caret>
}

// EXIST: { lookupString: "shade", itemText: "shade", tailText: "()", typeText: "Unit" }
// EXIST: { lookupString: "shade", itemText: "shade", tailText: "() for Shadow in <root>", typeText: "Unit" }
// NOTHING_ELSE