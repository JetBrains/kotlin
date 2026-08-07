// TARGET_BACKEND: WASM
// ENABLE_TAIL_CALLS

// Stress test for native Wasm tail call emission. Each pattern recurses at a depth that would
// stack-overflow on the host JS engine if the calls were not lowered to `return_call` / `return_call_ref`.

// Static mutual recursion. Direct top-level functions, all calls in tail position.
fun staticEven(n: Int): Boolean = if (n == 0) true else staticOdd(n - 1)
fun staticOdd(n: Int): Boolean = if (n == 0) false else staticEven(n - 1)


// Virtual dispatch tail call. Two open subclasses bounce through a virtual method.
abstract class VirtualParity {
    var partner: VirtualParity? = null
    abstract fun isMyKind(n: Int): Boolean
}

class VirtualEven : VirtualParity() {
    override fun isMyKind(n: Int): Boolean = if (n == 0) true else partner!!.isMyKind(n - 1)
}

class VirtualOdd : VirtualParity() {
    override fun isMyKind(n: Int): Boolean = if (n == 0) false else partner!!.isMyKind(n - 1)
}


// Interface dispatch tail call.
interface InterfaceParity {
    val partner: InterfaceParity?
    fun isMyKind(n: Int): Boolean
}

class InterfaceEven(override var partner: InterfaceParity? = null) : InterfaceParity {
    override fun isMyKind(n: Int): Boolean = if (n == 0) true else partner!!.isMyKind(n - 1)
}

class InterfaceOdd(override var partner: InterfaceParity? = null) : InterfaceParity {
    override fun isMyKind(n: Int): Boolean = if (n == 0) false else partner!!.isMyKind(n - 1)
}


// Single-self recursion that is not marked `tailrec`. The backend emits `return_call` for the self call.
fun selfSum(n: Int, acc: Long): Long = if (n == 0) acc else selfSum(n - 1, acc + n)


fun box(): String {
    val depth = 1_000_000

    if (!staticEven(depth)) return "fail static even"
    if (staticOdd(depth)) return "fail static odd"

    val ve = VirtualEven()
    val vo = VirtualOdd()
    ve.partner = vo
    vo.partner = ve
    if (!ve.isMyKind(depth)) return "fail virtual even"
    if (vo.isMyKind(depth)) return "fail virtual odd"

    val ie = InterfaceEven()
    val io = InterfaceOdd()
    ie.partner = io
    io.partner = ie
    if (!ie.isMyKind(depth)) return "fail interface even"
    if (io.isMyKind(depth)) return "fail interface odd"

    val expectedSum = depth.toLong() * (depth + 1) / 2
    if (selfSum(depth, 0L) != expectedSum) return "fail self sum"

    return "OK"
}
