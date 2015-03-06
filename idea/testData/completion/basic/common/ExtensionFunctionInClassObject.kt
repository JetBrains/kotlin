package p

class B

class R {
    default object {
        fun B.f() {
            this.<caret>
        }
    }
}

// EXIST: { itemText: "f", tailText: "() for B in R.Default" }
