class Factory {
    sealed class Function {
        object Default
    }

    companion object {
        val f = <!NO_COMPANION_OBJECT!>Function<!>
        val x = Function.Default
    }
}
