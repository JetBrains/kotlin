package test

interface Z {
    fun a() : String
}

inline fun test(crossinline z: () -> String) =
        object : Z {

            val p = z()

            override fun a() = p
        }


inline fun<T> call(crossinline z: () -> T) = z()