class A {
    operator fun A.minus(o: A) = o

    operator fun A.add(o: A) = o
    operator fun A.get(o: A) = o
    operator fun A.invokee() {}
}

operator fun A.plus(o: A) = o
operator fun A.component1() = 1
operator fun A.componentN() = 1