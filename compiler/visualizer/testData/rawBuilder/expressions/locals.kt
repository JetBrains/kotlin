fun withLocals(p: Int): Int {
    class Local(val pp: Int) {
//                   val (withLocals/Local).pp: Int
//                   │  fun (Int).minus(Int): Int
//                   │  │ withLocals.p: Int
//                   │  │ │
        fun diff() = pp - p
    }

//          constructor withLocals.Local(Int)
//      Int │     Int fun (withLocals/Local).diff(): Int
//      │   │     │   │
    val x = Local(42).diff()

    fun sum(y: Int, z: Int, f: (Int, Int) -> Int): Int {
//             val withLocals.x: Int
//             │ fun (Int).plus(Int): Int
//             │ │ fun ((Int, Int) -> Int).invoke(Int, Int): Int
//             │ │ │ withLocals.sum.y: Int
//             │ │ │ │ fun (Int).plus(Int): Int
//             │ │ │ │ │ withLocals.sum.z: Int
//             │ │ │ │ │ │
        return x + f(y + z)
    }

//      Int              constructor Any()
//      │                │
    val code = (object : Any() {
//                  fun (Any).hashCode(): Int
//                  │
        fun foo() = hashCode()
//     fun (withLocals/<no name provided>).foo(): Int
//     │
    }).foo()

//         fun withLocals.sum(Int, Int, (Int, Int) -> Int): Int
//         │   val withLocals.code: Int
//         │   │     constructor withLocals.Local(Int)
//         │   │     │     Int
//         │   │     │     │  fun (withLocals/Local).diff(): Int
//         │   │     │     │  │                             withLocals.<no name provided>.x: Int
//         │   │     │     │  │                             │ fun (Int).plus(Int): Int
//         │   │     │     │  │                             │ │ withLocals.<no name provided>.y: Int
//         │   │     │     │  │                             │ │ │
    return sum(code, Local(1).diff(), fun(x: Int, y: Int) = x + y)
}
