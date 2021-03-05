fun withLocals(p: Int): Int {
    class Local(val pp: Int) {
//                 Int
//                 │ val (withLocals.Local).pp: Int
//                 │ │  fun (Int).minus(Int): Int
//                 │ │  │ withLocals.p: Int
//                 │ │  │ │
        fun diff() = pp - p
    }

//          constructor withLocals.Local(Int)
//      Int │     Int fun (withLocals.Local).diff(): Int
//      │   │     │   │
    val x = Local(42).diff()

    fun sum(y: Int, z: Int, f: (Int, Int) -> Int): Int {
//             val withLocals.x: Int
//             │ fun (Int).plus(Int): Int
//             │ │ fun ((P1, P2) -> R).invoke(P1, P2): R
//             │ │ │ withLocals.sum.y: Int
//             │ │ │ │  withLocals.sum.z: Int
//             │ │ │ │  │
        return x + f(y, z)
    }

//      Int              constructor Any()
//      │                │
    val code = (object : Any() {
//                Int
//                │ fun (Any).hashCode(): Int
//                │ │
        fun foo() = hashCode()
//     fun (<anonymous>).foo(): Int
//     │
    }).foo()

//         fun withLocals.sum(Int, Int, (Int, Int) -> Int): Int
//         │   val withLocals.code: Int
//         │   │     constructor withLocals.Local(Int)
//         │   │     │     Int
//         │   │     │     │  fun (withLocals.Local).diff(): Int
//         │   │     │     │  │                           Int
//         │   │     │     │  │                           │ withLocals.<anonymous>.x: Int
//         │   │     │     │  │                           │ │ fun (Int).plus(Int): Int
//         │   │     │     │  │                           │ │ │ withLocals.<anonymous>.y: Int
//         │   │     │     │  │                           │ │ │ │
    return sum(code, Local(1).diff(), fun(x: Int, y: Int) = x + y)
}
