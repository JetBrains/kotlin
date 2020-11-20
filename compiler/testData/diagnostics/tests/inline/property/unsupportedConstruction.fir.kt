// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE -NOTHING_TO_INLINE

inline val z: Int
    get()  {

    class A {
        fun a() {
           class AInner {}
        }
    }

    <!LOCAL_OBJECT_NOT_ALLOWED!>object B<!>{
        <!LOCAL_OBJECT_NOT_ALLOWED!>object BInner<!> {}
    }

    fun local() {
        fun localInner() {}
    }
    return 1
}
