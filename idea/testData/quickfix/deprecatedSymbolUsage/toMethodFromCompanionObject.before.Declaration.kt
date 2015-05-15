package dependency

class C {
    @deprecated("", ReplaceWith("newFun(this)"))
    fun oldFun() {}

    companion object {
        fun newFun(c: C) {}
    }
}

