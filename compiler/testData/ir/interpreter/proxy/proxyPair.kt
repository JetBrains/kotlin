import kotlin.*
import kotlin.collections.*

@CompileTimeCalculation
class A(val a: Int)
const val size = <!EVALUATED: `1`!>mapOf(1 to "A(1)").size<!>
const val first = <!EVALUATED: `1`!>mapOf(1 to "A(1)").entries.single().key<!>
const val second = <!EVALUATED: `A(1)`!>mapOf(1 to "A(1)").values.single()<!>
