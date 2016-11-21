impl fun Int.plus(s: CharSequence): Int = 0
impl infix fun Int.minus(s: String): Int = 1

impl fun Double.times(x: CharArray) {}
impl operator fun Double.divide(x: ByteArray) {}

impl fun f1() {}
impl external fun g1()

impl fun f2() {}
impl inline fun g2() {}

impl fun f3() {}
impl tailrec fun g3() {}
