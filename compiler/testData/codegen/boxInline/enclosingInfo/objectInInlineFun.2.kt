package test

interface Z {
    fun a(): String
}

inline fun test(crossinline z: () -> String) =
        object : Z {
            override fun a() = z()
        }
