class Klass {
    companion object {
        val NAME = "Klass"
    }
}

trait Trait {
    companion object {
        val NAME = "Trait"
    }
}

enum class Enoom {
    companion object {
        val NAME = "Enoom"
    }
}

fun box() = Test().test()
