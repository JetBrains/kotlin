package c

class C<T>: <error>T</error> by <error>{
}</error>

class D<T>: <error>T</error> by<EOLError></EOLError>

class G<T> : <error>T</error> by <error>{

    val c = 3
}</error>

interface I

class A<T : I>(a: T) : <error>T</error> by a