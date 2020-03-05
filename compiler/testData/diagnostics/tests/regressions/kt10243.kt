// !WITH_NEW_INFERENCE
val f: Boolean = true
private fun doUpdateRegularTasks() {
    try {
        while (f) {
            val xmlText = <!UNRESOLVED_REFERENCE!>getText<!>()
            if (<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>xmlText<!> <!NI;DEBUG_INFO_MISSING_UNRESOLVED, OI;DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>==<!> null) {}
            else {
                <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>xmlText<!>.<!NI;DEBUG_INFO_MISSING_UNRESOLVED, NI;VARIABLE_EXPECTED, OI;DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>value<!> = 0 // !!!
            }
        }

    }
    finally {
        fun execute() {}
    }
}
