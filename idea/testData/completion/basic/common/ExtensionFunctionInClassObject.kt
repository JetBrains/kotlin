package p

class B

class R {
    class object {
        fun B.f() {
            this.<caret>
        }
    }
}

// EXIST: { itemText: "f", tailText: "() for B in R.Default" }
