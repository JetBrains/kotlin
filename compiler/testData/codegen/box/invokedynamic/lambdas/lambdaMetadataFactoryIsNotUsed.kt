// LAMBDAS: INDY
// TARGET_BACKEND: JVM_IR
// JVM_TARGET: 1.8
// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// JVM_IR_TEMPLATES
// 0 java/lang/invoke/LambdaMetafactory


fun box(): String {
    suspend {}

    fun (a: Any, b: Any, c: Any, d: Any, e: Any, f: Any, g: Any, h: Any, i: Any, j: Any, k: Any, l: Any, m: Any, n: Any, o: Any,
         p: Any, q: Any, r: Any, s: Any, t: Any, u: Any, v: Any, w: Any) = "one too many"

    fun Any.(a: Any, b: Any, c: Any, d: Any, e: Any, f: Any, g: Any, h: Any, i: Any, j: Any, k: Any, l: Any, m: Any, n: Any, o: Any,
             p: Any, q: Any, r: Any, s: Any, t: Any, u: Any, v: Any) = "one too many"

    { a: Any, b: Any, c: Any, d: Any, e: Any, f: Any, g: Any, h: Any, i: Any, j: Any, k: Any, l: Any, m: Any, n: Any, o: Any,
      p: Any, q: Any, r: Any, s: Any, t: Any, u: Any, v: Any, w: Any -> "one too many" }

    fun local(a: Any, b: Any, c: Any, d: Any, e: Any, f: Any, g: Any, h: Any, i: Any, j: Any, k: Any, l: Any, m: Any, n: Any, o: Any,
               p: Any, q: Any, r: Any, s: Any, t: Any, u: Any, v: Any, w: Any) = "one too many"
    { a: Any, b: Any, c: Any, d: Any, e: Any, f: Any, g: Any, h: Any, i: Any, j: Any, k: Any, l: Any, m: Any, n: Any, o: Any,
      p: Any, q: Any, r: Any, s: Any, t: Any, u: Any, v: Any, w: Any ->
        local(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, u, v, w)
    }

    return "OK"
}