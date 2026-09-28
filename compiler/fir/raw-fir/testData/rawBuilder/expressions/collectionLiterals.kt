annotation class Ann1(val arr: IntArray)

annotation class Ann2(val arr: DoubleArray)

annotation class Ann3(val arr: Array<String>)

@Ann1([])
@Ann2([])
@Ann3([])
class Zero

@Ann1([1, 2])
class First

@Ann2([3.14])
class Second

@Ann3(["Alpha", "Omega"])
class Third
