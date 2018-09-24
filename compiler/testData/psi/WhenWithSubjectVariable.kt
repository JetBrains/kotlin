fun test() {
    when (val x1 = foo) {}
    when (val x2t: T) {}
    when (val y2t: T = foo) {}
    when (val (x3, x4) = foo) {}
    when (val (y3t: T, y4) = foo) {}
    when (val (z3, z4t: T) = foo) {}
    when (val (w3t: T, w4t: T) = foo) {}
    when (val T.x5 = foo) {}
    when (val T1.x5t: T2 = foo) {}
    when (@Ann val x6a = foo) {}
    when (val x7a: @Ann T = foo) {}
    when (val x8a = @Ann foo) {}
}
