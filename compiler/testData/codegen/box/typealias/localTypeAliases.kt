// IGNORE_BACKEND_K1: ANY
// ISSUE: KT-79185
// LANGUAGE: +NestedTypeAliases +LocalTypeAliases

// MODULE: lib

open class Generic<K>(val k: K) {
    companion object C {
        const val prop: Int = 123
    }

    inner class Inner<K2>(val k2: K2)
}

fun testGeneral(): String {
    class Local2<T>(val p: T)

    open class Local {
        val p: String
            get() = "OK"

        typealias LocalTAtoLocal = Local
        typealias LocaTAtoLocal2<K> = Local2<K>
        typealias LocalTA2toLocal2 = Local2<String>

        fun test(): String {
            if (LocalTAtoLocal().p != "OK") return "FAIL"
            if (LocaTAtoLocal2("OK").p != "OK") return "FAIL"
            if (LocalTA2toLocal2("OK").p != "OK") return "FAIL"
            return "OK"
        }
    }

    typealias TAtoLocal = Local
    typealias TAtoLocal2<K> = Local2<K>
    typealias TA2toLocal2 = Local2<String>

    typealias TAtoGeneric<K> = Generic<K>
    typealias TAtoInner<K, L> = Generic<K>.Inner<L>

    if (TAtoLocal().p != "OK") return "FAIL"
    if (TAtoLocal2("OK").p != "OK") return "FAIL"
    if (TA2toLocal2("OK").p != "OK") return "FAIL"

    val callable = ::TAtoLocal
    if (callable().p != "OK") return "FAIL"

    fun localFunc(): TAtoLocal = TAtoLocal()
    if (localFunc().test() != "OK") return "FAIL"

    if (TAtoGeneric<String>("OK").k != "OK") return "FAIL"
    val genericObj = Generic<Int>(42)
    if (genericObj.TAtoInner<Int, String>("OK").k2 != "OK") return "FAIL"

    val anonObject = object : TAtoLocal() {}
    if (anonObject.test() != "OK") return "FAIL"

    val anonObject2 = object : TAtoGeneric<String>("OK") {}
    if (anonObject2.k != "OK") return "FAIL"

    val companion = TAtoGeneric
    if (companion.prop != 123) return "FAIL"

    val typeRefToLocalTypeAliasInLocalClass: Local.LocalTAtoLocal = Local.LocalTAtoLocal()
    if (typeRefToLocalTypeAliasInLocalClass.p != "OK") return "FAIL"

    return "OK"
}

abstract class A {
    abstract val p: String
}

fun testReturnTypeAsLocalTypeAlias(): A {
    typealias TA = String
    class B(override val p: TA) : A()
    return B("OK")
}

// MODULE: main(lib)

fun box(): String {
    if (testGeneral() != "OK") return "FAIL"
    if (testReturnTypeAsLocalTypeAlias().p != "OK") return "FAIL"
    return "OK"
}