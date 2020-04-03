package b

import a.A
import a.AA
import a.AAA

interface B2 {
    fun consumeA(a: A<Int, String, Double>.Inner<B2>)
    fun consumeAA(a: AA<Int, Unit>.Inner<String>)
    fun consumeAAA(a: AAA<String>.Inner<Int>.InnerInner<Unit>)
}
