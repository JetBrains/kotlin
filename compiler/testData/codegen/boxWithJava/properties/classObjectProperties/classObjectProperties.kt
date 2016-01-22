class Klass {
    companion object {
        const val NAME = "Klass"
        @JvmField val JVM_NAME = "JvmKlass"
    }
}

interface Trait {
    companion object {
        const val NAME = "Trait"
    }
}

enum class Enoom {
    ;
    companion object {
        const val NAME = "Enum"
        @JvmField val JVM_NAME = "JvmEnum"
    }
}

fun box() = Test().test()
