// See KT-14453
val <T : Any> KClass1<T>.primaryConstructor: KFunction1<T>? get() = null!!

interface KClass1<F : Any>
interface KFunction1<out R>
fun f(type: KClass1<*>): KFunction1<Any>? =
        // What happens here:
        // 1. Create captured type for type argument T (it's built upon star-projection from `KClass<*>` that's argument is <: Any)
        // 2. Approximate resulting descriptor, now it 'KClass1<*>.primaryConstructor: KFunction1<*>'
        //    Note that star-projection in 'KFunction1<*>' is obtained from KClass and its `projectionType` is Any (not-nullable).
        //    Of course that situation is already strange
        // 3. When checking subtyping after call completion we get that 'KFunction1<*>' is not a subtype of 'KFunction1<Any>' in new type checker.
        //    The answer is correct, but this old type checker used `projectionType` for this and it was resulting in KFunction1<*> <: KFunction1<Any>
        //
        // So to fix the issue we use 'out starProjectionType' instead of star-projection itself in approximation
        // Note that in new inference there should be no approximation for IN-position types (they must remain captured)
        type.primaryConstructor
