import kotlin.*
import kotlin.collections.*

@CompileTimeCalculation
class A(val a: Int)
const val size = mapOf(1 to "A(1)").<!EVALUATED: `1`!>size<!>
const val first = mapOf(1 to "A(1)").entries.single().<!EVALUATED: `1`!>key<!>
const val second = mapOf(1 to "A(1)").values.<!EVALUATED: `A(1)`!>single()<!>
