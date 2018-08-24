// !LANGUAGE: +InlineClasses

inline class Z1(val x: Int)
inline class Z2(val x: Z1)

fun test(zs: MutableList<Z2>, z: Z2) {
    zs.add(z)
}

// 1 public final static box\(I\)LZ2;
// 1 INVOKESTATIC Z2\$Erased.box \(I\)LZ2;