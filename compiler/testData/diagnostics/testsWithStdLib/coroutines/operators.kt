// !DIAGNOSTICS: -UNUSED_PARAMETER
// SKIP_TXT

class A {
    suspend <!UNSUPPORTED!>operator<!> fun get(x: Int) = 1
    suspend <!UNSUPPORTED!>operator<!> fun set(x: Int, v: String) {}

    <!UNSUPPORTED!>operator<!> suspend fun contains(y: String): Boolean = true
}

class B
suspend <!UNSUPPORTED!>operator<!> fun B.get(x: Int) =1
suspend <!UNSUPPORTED!>operator<!> fun B.set(x: Int, v: String) {}

<!UNSUPPORTED!>operator<!> suspend fun B.contains(y: String): Boolean = true

class C {
    suspend fun get(x: Int) = 1
    suspend fun set(x: Int, v: String) {}

    suspend fun contains(y: String): Boolean = true
}

class D
suspend fun D.get(x: Int) =1
suspend fun D.set(x: Int, v: String) {}

suspend fun D.contains(y: String): Boolean = true
