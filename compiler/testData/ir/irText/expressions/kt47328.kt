// SKIP_KT_DUMP
// SKIP_KLIB_TEST
// WITH_RUNTIME

interface A { val x: Int }

class B(@JvmField override val x: Int): A

class C<D: A>(@JvmField val d: D)

class E(c: C<B>) { val ax = c.d.x }
