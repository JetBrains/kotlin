interface I1
interface I2
interface I3
interface I4
interface I5

<info descr="null">operator</info> fun I1.component1() = 1
<info descr="null">operator</info> fun I2.component2() = 2
<info descr="null">operator</info> fun I3.component3() = 3
<info descr="null">operator</info> fun I4.component4() = 4
<info descr="null">operator</info> fun I5.component5() = 5

fun test(x: Any): Int {
    if (x is I1 && x is I2 && x is I3 && x is I4 && x is I5) {
        val (t1, t2, t3, t4, t5) = <info descr="Smart cast to I1 (for t1 call)"><info descr="Smart cast to I2 (for t2 call)"><info descr="Smart cast to I3 (for t3 call)"><info descr="Smart cast to I4 (for t4 call)"><info descr="Smart cast to I5 (for t5 call)">x</info></info></info></info></info>
        return t1 + t2 + t3 + t4 + t5
    }
    else return 0
}
