interface I

class C : I {
    inner class Inner {
        fun foo() {
            xx<caret>
        }

        val Any.xxx: Int get() = 1
    }

    val I.xxx: Int get() = 1

}

val C.xxx: Int get() = 1

// EXIST: { lookupString: "xxx", itemText: "xxx", tailText: " for Any in C.Inner", typeText: "Int" }
// NOTHING_ELSE
