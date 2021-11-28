// WITH_STDLIB
// TARGET_BACKEND: JVM

private const val MOD = 998244353

private fun mul(a: Int, b: Int) = (a.toLong() * b % MOD).toInt()

fun box(): String {
    val n = 400
    val d = Array(n) { IntArray(n) { Int.MAX_VALUE / 2 } }
    for (i in 0 until n) {
        d[i][i] = 0
    }
    val m = n - 1
    val g = Graph(n, 2 * m)
    repeat(m) {
        val a = it
        val b = it + 1
        d[a][b] = 1
        d[b][a] = 1
        g.add(a, b)
        g.add(b, a)
    }
    for (k in 0 until n) {
        for (i in 0 until n) {
            for (j in 0 until n) {
                val s = d[i][k] + d[k][j]
                if (s < d[i][j]) d[i][j] = s
            }
        }
    }
    for (x in 0 until n) {
        val row = IntArray(n) { y ->
            var prod = 1
            val dx = d[x]
            val xy = dx[y]
            for (k in 0 until n) if (k != x) {
                val dy = d[y]
                val xk = dx[k]
                val yk = dy[k]
                var cnt = 0
                var cntMid = 0
                g.from(k) { t ->
                    val xt = dx[t]
                    val yt = dy[t]
                    if (xt == xk - 1) when (yt) {
                        yk - 1 -> {
                            cnt++
                        }
                        yk + 1 -> {
                            if (xk + yk == xy) {
                                cntMid++
                                cnt++
                            }
                        }
                    }
                }
                if (cntMid > 1 || cnt == 0) {
                    prod = 0
                    break
                } else {
                    prod = mul(prod, cnt)
                }
            }
            prod
        }
        for (i in 0 until n) {
            if (row[i] != 1) throw AssertionError("x: $x; row[$i]: ${row[i]}")
        }
    }

    return "OK"
}

class Graph(vCap: Int = 16, eCap: Int = vCap * 2) {
    var vCnt = 0
    var eCnt = 0
    var vHead = IntArray(vCap) { -1 }
    var eVert = IntArray(eCap)
    var eNext = IntArray(eCap)

    fun add(v: Int, u: Int, e: Int = eCnt++) {
        ensureVCap(maxOf(v, u) + 1)
        ensureECap(e + 1)
        eVert[e] = u
        eNext[e] = vHead[v]
        vHead[v] = e
    }

    inline fun from(v: Int, action: (u: Int) -> Unit) {
        var e = vHead[v]
        while (e >= 0) {
            action(eVert[e])
            e = eNext[e]
        }
    }

    private fun ensureVCap(vCap: Int) {
        if (vCap <= vCnt) return
        vCnt = vCap
        if (vCap > vHead.size) {
            val newSize = maxOf(2 * vHead.size, vCap)
            vHead = vHead.copyOf(newSize)
        }
    }

    private fun ensureECap(eCap: Int) {
        if (eCap <= eCnt) return
        eCnt = eCap
        if (eCap > eVert.size) {
            val newSize = maxOf(2 * eVert.size, eCap)
            eVert = eVert.copyOf(newSize)
            eNext = eNext.copyOf(newSize)
        }
    }
}
