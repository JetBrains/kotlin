// FILE: main.kt
import dependency.Bar.property
import dependency.Bar.function
import dependency.Bar.callable

fun test() {
    property
    function()
    ::callable
}

// FILE: dependency.kt
package dependency

object Bar {
    val property: Int = 10
    fun function() {}
    fun callable() {}
}
