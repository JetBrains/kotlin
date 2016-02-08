package test

inline fun foo(x: String) = x

inline fun processRecords(s: String?, block: String.(String) -> String) = s?.block(foo("O"))
