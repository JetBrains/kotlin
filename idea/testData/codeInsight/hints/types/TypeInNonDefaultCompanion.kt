// MODE: property
class A {
    companion object N {
        class InA
        fun provideInA() = InA()
    }
}
val inA<# : A.N.InA #> = A.provideInA()