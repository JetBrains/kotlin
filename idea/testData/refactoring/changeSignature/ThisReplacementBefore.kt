// CHECK_ERRORS_AFTER
class C {
    fun f() {
        fun Any.<caret>local() {
            if (this is String) {
                this.length
            }
        }
    }
}