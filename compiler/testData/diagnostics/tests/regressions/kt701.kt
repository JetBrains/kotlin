// KT-702 Type inference failed
fun getJavaClass<T>() : java.lang.Class<T> { return "" <!CAST_NEVER_SUCCEEDS!>as<!> Class<T> }

public class Throwables() {
    default object {
        public fun propagateIfInstanceOf<X : Throwable?>(throwable : Throwable?, declaredType : Class<X<!BASE_WITH_NULLABLE_UPPER_BOUND!>?<!>>?) {
            if (((throwable != null) && declaredType?.isInstance(throwable)!!))
            {
                throw declaredType?.cast(throwable)!!
            }
        }
        public fun propagateIfPossible(throwable : Throwable?) {
            propagateIfInstanceOf(throwable, getJavaClass<Error?>()) //; Type inference failed: Mismatch while expanding constraints
            propagateIfInstanceOf(throwable, getJavaClass<RuntimeException?>()) // Type inference failed: Mismatch while expanding constraints
        }
    }
}
