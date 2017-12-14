// !WITH_NEW_INFERENCE
val f: Boolean = true
private fun doUpdateRegularTasks() {
    try {
        while (f) {
            val xmlText = <!UNRESOLVED_REFERENCE!>getText<!>()
            if (<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>xmlText<!> <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>==<!> null) {}
            else {
                <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>xmlText<!>.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>value<!> = 0 // !!!
            }
        }

    }
    finally {
        fun execute() {}
    }
}
