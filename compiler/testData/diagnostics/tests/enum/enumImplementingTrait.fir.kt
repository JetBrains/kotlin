interface T1 {
    fun foo()
}

enum class EnumImplementingTraitWithFun: T1 {
    E1 {
        override fun foo() {}
    },
    E2
}

interface T2 {
    val bar: Int
}

enum class EnumImplementingTraitWithVal: T2 {
    E1 {
        override val bar = 1
    },
    E2
}