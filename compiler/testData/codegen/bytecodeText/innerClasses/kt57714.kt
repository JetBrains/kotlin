// TARGET_BACKEND: JVM_IR

val x = createImpl<List<String>>()

interface IfaceWithGenericFun<A> {
    fun <B : A> doStuff(x: B)
}

inline fun <reified A> createImpl(): IfaceWithGenericFun<A> {
    return object : IfaceWithGenericFun<A> {
        override fun <B : A> doStuff(x: B) {}
    }
}

// 3 INNERCLASS
// 2 INNERCLASS Kt57714Kt\$createImpl\$1 null null
// 1 INNERCLASS Kt57714Kt\$special\$\$inlined\$createImpl\$1 null null