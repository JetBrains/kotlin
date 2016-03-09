package b

import a.A
import a.AA

interface B1 {
    fun produceA(): A<String>.Inner<Int, Unit>
    fun produceAA(): AA<Int>.Inner<Unit, String>
}
