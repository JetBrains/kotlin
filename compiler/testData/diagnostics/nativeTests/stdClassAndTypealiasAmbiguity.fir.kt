import kotlin.*
import kotlin.jvm.*
import kotlin.native.concurrent.*
import kotlin.native.*

@<!DEPRECATION!>SharedImmutable<!>
@ThreadLocal
val x = 42

@Throws(Exception::class)
fun test() {}
