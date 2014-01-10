class A {
}

package n {
  class B
}
abstract class XXX() {
    abstract val a : Int
    abstract val a1 : package.Int
    abstract val a2 : n.B
    abstract val a3 : (A)
    abstract val a31 : (n.B)
    abstract val a4 : A?
    abstract val a5 : (A)?
    abstract val a6 : (A?)
    abstract val a7 : (A) -> n.B
    abstract val a8 : (A, n.B) -> n.B

//val a9 : (A, B)
//val a10 : (B)? -> B

    val a11 : ((Int) -> Int)? = null
    val a12 : ((Int) -> (Int))? = null
    abstract val a13 : Int.(Int) -> Int
    abstract val a14 : n.B.(Int) -> Int
    abstract val a15 : Int? .(Int) -> Int
    abstract val a152 : (Int?).(Int) -> Int
    abstract val a151 : Int?.(Int) -> Int
    abstract val a16 : (Int) -> (Int) -> Int
    abstract val a17 : ((Int) -> Int).(Int) -> Int
    abstract val a18 : (Int) -> ((Int) -> Int)
    abstract val a19 : ((Int) -> Int) -> Int
}

abstract class YYY() {
    abstract val a7 : (a : A) -> n.B
    abstract val a8 : (a : A, b : n.B) -> n.B
//val a9 : (A, B)
//val a10 : (B)? -> B
    val a11 : ((a : Int) -> Int)? = null
    val a12 : ((a : Int) -> (Int))? = null
    abstract val a13 : Int.(a : Int) -> Int
    abstract val a14 : n.B.(a : Int) -> Int
    abstract val a15 : Int? .(a : Int) -> Int
    abstract val a152 : (Int?).(a : Int) -> Int
abstract val a151 : Int?.(a : Int) -> Int
    abstract val a16 : (a : Int) -> (a : Int) -> Int
    abstract val a17 : ((a : Int) -> Int).(a : Int) -> Int
    abstract val a18 : (a : Int) -> ((a : Int) -> Int)
    abstract val a19 : (b : (a : Int) -> Int) -> Int
}
