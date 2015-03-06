class Klass {
    default object {
        val NAME = "Klass"
    }
}

trait Trait {
    default object {
        val NAME = "Trait"
    }
}

enum class Enoom {
    default object {
        val NAME = "Enoom"
    }
}

fun box() = Test().test()
