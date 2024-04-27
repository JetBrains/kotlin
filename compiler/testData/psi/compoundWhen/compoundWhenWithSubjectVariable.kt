fun test() {
    when (val x1 = foo; x1) {}
    when (val x2t: T; x2t) {}
    when (val y2t: T = foo; y2t) {}
    when (val (x3, x4) = foo; x3) {}
    when (val (y3t: T, y4) = foo; y3t) {}
    when (val (z3, z4t: T) = foo; z3) {}
    when (val (w3t: T, w4t: T) = foo; w3t) {}
    when (val T.x5 = foo; T.x5) {}
    when (val T1.x5t: T2 = foo; T1.x5t) {}
    when (@Ann val x6a = foo; x6a) {}
    when (val x7a: @Ann T = foo; x7a) {}
    when (val x8a = @Ann foo; x8a) {}
}
