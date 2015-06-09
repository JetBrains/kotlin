class C {
    val xxx = 1

    fun foo(xxx: String) {
        val xxx = 'x'

        if (true) {
            val xxx = true
            xx<caret>
        }
    }
}

// EXIST: { lookupString: "xxx", itemText: "xxx", typeText: "Boolean" }
// NOTHING_ELSE
