interface T
class T1(<warning>t</warning>: Int): T

inline fun <T> run(f: () -> T) = f()


class Delegate(<warning>d</warning>: Int) {
    fun get(k: Any, m: PropertyMetadata) {}
}

class A(y: Int, t: Int, d: Int): T <info>by</info> T1(t) {
    val <info>a</info> = y
    val b <info>by</info> Delegate(d)
}

class A2<T>(x: Int, y: Int, t: T) {
    val <info>t1</info>: T = t

    val <info>x1</info> = run { x }
    <info>init</info> {
        run {
            y
        }
    }
}


//captured

class B(
        <info descr="Value captured in a closure">x</info>: Int,
        <info descr="Value captured in a closure">y</info>: Int,
        <info descr="Value captured in a closure">t</info>: Int,
        <info descr="Value captured in a closure">d</info>: Int
) {
    <info>init</info> {
        class C(<warning>a</warning>: Int = <info>x</info>): T <info>by</info> T1(<info>t</info>) {
            val <info>a</info> = <info>y</info>
            val b <info>by</info> Delegate(<info>d</info>)
        }
    }
}

class B2(<info descr="Value captured in a closure">x</info>: Int, <info descr="Value captured in a closure">y</info>: Int) {

    val <info>x1</info> =  { <info>x</info> }()
    <info>init</info> {
        {
            <info>y</info>
        }()
    }
}
