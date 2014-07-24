package test

public class Z(public var s: Int)

inline fun Z.plusAssign(lambda: () -> Int)  {
    this.s += lambda()
}
