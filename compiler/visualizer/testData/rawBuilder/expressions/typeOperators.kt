// FIR_IGNORE
interface IThing

//                Boolean
//                │ test1.x: Any
//                │ │
fun test1(x: Any) = x is IThing
//                Boolean
//                │ test2.x: Any
//                │ │
fun test2(x: Any) = x !is IThing
//                IThing
//                │ test3.x: Any
//                │ │
fun test3(x: Any) = x as IThing
//                IThing?
//                │ test4.x: Any
//                │ │
fun test4(x: Any) = x as? IThing
