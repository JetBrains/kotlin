package test

expect class Simple
expect fun createSimple(): Simple

expect class Generic<A, B>
expect fun <A, B> createGeneric(a: A, b: B): Generic<A, B>
