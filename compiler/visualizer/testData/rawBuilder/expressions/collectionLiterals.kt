annotation class Ann1(val arr: IntArray)

annotation class Ann2(val arr: DoubleArray)

annotation class Ann3(val arr: Array<String>)

//constructor Ann1(IntArray)
//│
@Ann1([])
//constructor Ann2(DoubleArray)
//│
@Ann2([])
//constructor Ann3(Array<String>)
//│
@Ann3([])
class Zero

//constructor Ann1(IntArray)
//│    Int
//│    │  Int
//│    │  │
@Ann1([1, 2])
class First

//constructor Ann2(DoubleArray)
//│    Double
//│    │
@Ann2([3.14])
class Second

//constructor Ann3(Array<String>)
//│
@Ann3(["Alpha", "Omega"])
class Third
