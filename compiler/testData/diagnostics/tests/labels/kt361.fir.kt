fun nonlocals(b : Boolean) {
    a@{
        fun foo() {
            if (b) {
                return@a 1 // The label must be resolved, but an error should be reported for a non-local return
            }
        }

        return@a 5
    }
}