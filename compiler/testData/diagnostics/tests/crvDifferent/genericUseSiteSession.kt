// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

// MODULE: lib1
// RETURN_VALUE_CHECKER_MODE: CHECKER

// FILE: Lib.kt

interface Box<T> {
    fun add(t: T): Boolean
}

interface A<T> {
    fun foo(): T
}

interface B : A<String>

// MODULE: main(lib1)
// RETURN_VALUE_CHECKER_MODE: FULL

// FILE: App.kt

interface C: A<Int>

interface D: A<String> {
    override fun foo(): String {
        return ""
    }
}

@MustUseReturnValues
interface BoxImpl: Box<String> {
    override fun add(t: String): Boolean {
        return true
    }
}

fun useBox(box: Box<String>, b: B, c: C, d: D, i: BoxImpl) {
    // Should not be reported because Lib is compiled in CHECKER mode:
    box.add("")
    b.foo()
    // questionable, but let's leave it for now:
    c.foo()
    d.foo()
    i.add("")
}

/* GENERATED_FIR_TAGS: functionDeclaration, interfaceDeclaration, nullableType, override, stringLiteral, typeParameter */
