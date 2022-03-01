package foo

interface A {
    fun foo()
}
interface B

// A: FirUserTypeRef("A")
//    FirResolvedTypeRef(
//        type = ConeClassType(
//            classId = foo.A,
//            typeArguments = []
//        )
//    )

class C : A, B

val x: A = C()
