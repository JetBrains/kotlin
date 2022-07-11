// LAMBDAS: INDY
// TARGET_BACKEND: JVM_IR
// JVM_TARGET: 1.8
// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// JVM_IR_TEMPLATES
// 14 java/lang/invoke/LambdaMetafactory


fun box(): String {
    var captureMe = 0

    fun () {} // 1
    fun (a: Any, b: Any, c: Any, d: Any, e: Any, f: Any, g: Any, h: Any, i: Any, j: Any, k: Any, l: Any, m: Any, n: Any, o: Any,
         p: Any, q: Any, r: Any, s: Any, t: Any, u: Any, v: Any) = "just enough" // 2
    fun () = ++captureMe // 3
    fun (vararg x: Int) = x // 4

    fun Any.() {} // 5
    fun Any.(a: Any, b: Any, c: Any, d: Any, e: Any, f: Any, g: Any, h: Any, i: Any, j: Any, k: Any, l: Any, m: Any, n: Any, o: Any,
             p: Any, q: Any, r: Any, s: Any, t: Any, u: Any) = "just enough" // 6
    fun () = ++captureMe // 7
    fun Any.(vararg x: Int) = x // 8


    {} // 9
    { a: Any, b: Any, c: Any, d: Any, e: Any, f: Any, g: Any, h: Any, i: Any, j: Any, k: Any, l: Any, m: Any, n: Any, o: Any,
      p: Any, q: Any, r: Any, s: Any, t: Any, u: Any, v: Any -> "just enough" } // 10
    { ++captureMe } // 11


    fun local1() {}
    { local1() } // 12

    fun local2(a: Any, b: Any, c: Any, d: Any, e: Any, f: Any, g: Any, h: Any, i: Any, j: Any, k: Any, l: Any, m: Any, n: Any, o: Any,
         p: Any, q: Any, r: Any, s: Any, t: Any, u: Any, v: Any) = "just enough"
    { a: Any, b: Any, c: Any, d: Any, e: Any, f: Any, g: Any, h: Any, i: Any, j: Any, k: Any, l: Any, m: Any, n: Any, o: Any,
      p: Any, q: Any, r: Any, s: Any, t: Any, u: Any, v: Any ->
        local2(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, u, v)
    } // 13

    fun local3() = ++captureMe
    { local3() } // 14

    return "OK"
}