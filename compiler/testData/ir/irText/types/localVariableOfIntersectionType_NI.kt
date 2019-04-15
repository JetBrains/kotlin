// !LANGUAGE: +NewInference
// With -NewInference we have
// TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS: Type inference failed: Cannot infer type parameter T in fun <T> create(x: In<T>, y: In<T>): Inv<T>
//      None of the following substitutions
//      (In<IA>,In<IA>)
//      (In<IB>,In<IB>)
//      can be applied to
//      (In<IA>,In<IB>)

interface In<in T>

interface Inv<T> {
    val t: T
}

interface Z {
    fun <T> create(x: In<T>, y: In<T>): Inv<T>
}

interface IA {
    fun foo()
}

interface IB {
    fun bar()
}

fun test(a: In<IA>, b: In<IB>, z: Z) {
    z.create(a, b).t.foo()
    z.create(a, b).t.bar()
    val t = z.create(a, b).t
    t.foo()
    t.bar()
}
