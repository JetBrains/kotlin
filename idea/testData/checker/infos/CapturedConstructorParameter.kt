// FIR_IDENTICAL
<info>import</info> kotlin.reflect.KProperty

interface T
class T1(<warning>t</warning>: Int): T

<info descr="null">inline</info> fun <T> run(f: () -> T) = f()


class Delegate(<warning>d</warning>: Int) {
    <info>operator</info> fun getValue(k: Any, m: KProperty<*>) {}
}

class A(y: Int, t: Int, d: Int): T <info>by</info> T1(t) {
    val a = y
    val b <info>by</info> Delegate(d)
}

class A2<T>(x: Int, y: Int, t: T) {
    val t1: T = t

    val x1 = run { x }
    <info>init</info> {
        run {
            y
        }
    }
}


//captured

class B(
        x: Int,
        y: Int,
        t: Int,
        d: Int
) {
    <info>init</info> {
        class C(<warning>a</warning>: Int = <info>x</info>): T <info>by</info> T1(<info>t</info>) {
            val a = <info>y</info>
            val b <info>by</info> Delegate(<info>d</info>)
        }
    }
}

class B2(x: Int, y: Int) {

    val x1 =  { <info>x</info> }()
    <info>init</info> {
        {
            <info>y</info>
        }()
    }
}
