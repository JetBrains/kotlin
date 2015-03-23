class C {
    fun f1() {
    }

    /**
     * The [f<caret>1] references a parameter.
     */
    fun f2(f1: String) {
    }
}

// REF: f1
