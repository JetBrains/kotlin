// TARGET_BACKEND: NATIVE
// FILECHECK_STAGE: CStubs

value class A(val i: Int)
value class B(val a: A)
value class C(val s: String)

fun defaultInt(a: Int = 1, aa: Int = 1) = a
fun defaultA(a: A = A(1), aa: A = A(1)) = a.i
fun defaultB(b: B = B(A(1)), bb: B = B(A(1))) = b.a.i
fun defaultC(c: C = C("1"), cc: C = C("1")) = c.s

// CHECK-LABEL: define %struct.ObjHeader* @"kfun:#box(){}kotlin.String"
fun box(): String {
    // CHECK-LABEL: entry
    // CHECK-NOT: <Int-box>
    // CHECK-NOT: <A-box>
    // CHECK-NOT: <B-box>
    // CHECK-NOT: <C-box>

    defaultInt()
    defaultA()
    defaultB()
    defaultC()
    defaultInt(1)
    defaultA(A(1))
    defaultB(B(A(1)))
    defaultC(C("1"))
    defaultInt(1, 1)
    defaultA(A(1), A(1))
    defaultB(B(A(1)), B(A(1)))
    defaultC(C("1"), C("1"))
    // CHECK-LABEL: epilogue:
    return "OK"
}