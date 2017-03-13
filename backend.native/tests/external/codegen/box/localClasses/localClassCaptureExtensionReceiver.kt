class Outer {
    fun String.id(): String {
        class Local(unused: Long) {
            fun result() = this@id
            fun outer() = this@Outer
        }

        return Local(42L).result()
    }

    fun result(): String = "OK".id()
}

fun box() = Outer().result()
