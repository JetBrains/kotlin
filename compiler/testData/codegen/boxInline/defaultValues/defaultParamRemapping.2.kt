package test

inline fun a(s1: String = "s1", s2: String = "s2", body: (a1: String, a2: String) -> String) = body(s1, s2)

inline fun String.b(body: (a1: String, a2: String) -> String) = a(s2 = this,  body = body)