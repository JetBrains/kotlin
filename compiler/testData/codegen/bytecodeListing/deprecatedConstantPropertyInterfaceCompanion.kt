interface A {
    companion object {
        @Deprecated("no")
        const val s = "yes"
    }
}

class B {
    companion object {
        @Deprecated("no")
        const val s = "yes"
    }
}