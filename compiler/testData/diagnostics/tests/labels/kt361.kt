fun nonlocals(b : Boolean) {
    @a<!UNUSED_FUNCTION_LITERAL!>{(): Int ->
        fun foo() {
            if (b) {
                <!RETURN_NOT_ALLOWED!>return@a 1<!> // The label must be resolved, but an error should be reported for a non-local return
            }
        }

        return@a 5
    }<!>
}