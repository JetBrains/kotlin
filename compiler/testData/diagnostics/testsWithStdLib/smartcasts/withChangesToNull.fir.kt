fun foo(y: String?) {
    var x: String? = ""
    if (x != null) {
        with(y?.let { x = null; it }) {
            this.<!INAPPLICABLE_CANDIDATE!>length<!>
            x.length
        }
        x.length
    }
}
