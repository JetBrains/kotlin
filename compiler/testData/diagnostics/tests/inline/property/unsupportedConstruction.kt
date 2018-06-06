// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE -NOTHING_TO_INLINE

inline val z: Int
    get()  {

    <!NOT_YET_SUPPORTED_IN_INLINE!>class<!> A {
        fun a() {
           class AInner {}
        }
    }

    <!LOCAL_OBJECT_NOT_ALLOWED!>object B<!>{
        <!LOCAL_OBJECT_NOT_ALLOWED!>object BInner<!> {}
    }

    <!NOT_YET_SUPPORTED_IN_INLINE!>fun<!> local() {
        fun localInner() {}
    }
    return 1
}
