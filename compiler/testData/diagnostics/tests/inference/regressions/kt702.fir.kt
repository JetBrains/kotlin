//KT-702 Type inference failed
package a
//+JDK

fun <T> getJavaClass() : java.lang.Class<T> { <!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>

public class Throwables() {
    companion object {
        public fun <X : Throwable?> propagateIfInstanceOf(throwable : Throwable?, declaredType : Class<X?>?) : Unit {
            if (((throwable != null) && declaredType?.isInstance(throwable)!!))
            {
                throw declaredType?.cast(throwable)!!
            }
        }
        public fun propagateIfPossible(throwable : Throwable?) : Unit {
            propagateIfInstanceOf(throwable, getJavaClass<Error?>()) // Type inference failed: Mismatch while expanding constraints
            propagateIfInstanceOf(throwable, getJavaClass<RuntimeException?>()) // Type inference failed: Mismatch while expanding constraints
        }
    }
}
