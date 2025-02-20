// FIR_IDENTICAL
import kotlin.properties.Delegates

private var value by Delegates.notNull<Int>()

fun main() {
    println(::value.<!LATEINIT_INTRINSIC_CALL_ON_NON_LATEINIT!>isInitialized<!>)
}
