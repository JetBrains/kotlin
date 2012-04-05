import testData.libraries.*

val v = 5.filter { it % 2 == 1 }

// library.kt
//public inline fun <T> T.<1>filter(predicate: (T)-> Boolean) : T? = this

