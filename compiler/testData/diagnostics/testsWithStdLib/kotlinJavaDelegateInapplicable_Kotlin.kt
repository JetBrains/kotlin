// ISSUE: KT-57543
// RENDER_DIAGNOSTICS_MESSAGES

class KotlinClass {

    class Val<T>(private final val initializer: Function0<T>) {
        operator fun getValue(instance: Any, metadata: Any): T {
            return initializer.invoke()
        }
    }

    companion object {
        fun <T> lazySoft(initializer: Function0<T>): Val<T> {
            return Val<T>(initializer)
        }
    }
}

class A(
    val c: Int? = 0,
    myType: (() -> Int)? = null
) {
    val arguments: A by KotlinClass.lazySoft {
        A(myType = if (false) null else fun(): Int { return c!! })
    }
}
