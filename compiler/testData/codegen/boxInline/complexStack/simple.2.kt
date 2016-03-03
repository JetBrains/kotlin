package test

inline fun foo(x: String) = x

inline fun processRecords(block: (String) -> String) = block(foo("O"))