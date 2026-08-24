class WithNamedCompanion {
    companion object Factory {
        fun create(): WithNamedCompanion = WithNamedCompanion()
    }
}

class WithDefaultCompanion {
    companion object {
        val shared: Int = 0
    }
}

class WithPrivateCompanion {
    private companion object
}
