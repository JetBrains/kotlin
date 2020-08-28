// MODE: property
class A {
    companion object {
        class InA
        fun provideInA() = InA()
    }
}
val inA<# : A.InA# > = A.provideInA()