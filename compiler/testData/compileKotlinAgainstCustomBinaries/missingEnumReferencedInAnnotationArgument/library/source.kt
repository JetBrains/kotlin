package test

enum class E { ENTRY }

annotation class Anno(val e: E)

@Anno(E.ENTRY) open class Class
