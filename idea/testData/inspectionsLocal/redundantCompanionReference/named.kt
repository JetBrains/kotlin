class C {
    companion object Obj {
        fun create() = C()
    }
}

fun test() {
    C.<caret>Obj.create()
}