// !WITH_NEW_INFERENCE
val f: Boolean = true
private fun doUpdateRegularTasks() {
    try {
        while (f) {
            val xmlText = <!UNRESOLVED_REFERENCE!>getText<!>()
            if (xmlText == null) {}
            else {
                xmlText.<!UNRESOLVED_REFERENCE!>value<!> = 0 // !!!
            }
        }

    }
    finally {
        fun execute() {}
    }
}
