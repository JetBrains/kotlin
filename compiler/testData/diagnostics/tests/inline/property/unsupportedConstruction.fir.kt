// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE -NOTHING_TO_INLINE

inline val z: Int
    get()  {

    class A {
        fun a() {
           class AInner {}
        }
    }

    object B{
        object BInner {}
    }

    fun local() {
        fun localInner() {}
    }
    return 1
}
