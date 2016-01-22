package test

interface Z<T> {
    fun a() : T
}

inline fun test(crossinline z: () -> String) =
        object : Z<Z<String>> {

            val p: Z<String> = object : Z<String> {

                val p2 = z()

                override fun a() = p2
            }

            override fun a() = p
        }
