package test

header class Simple
header fun createSimple(): Simple

header class Generic<A, B>
header fun <A, B> createGeneric(a: A, b: B): Generic<A, B>
