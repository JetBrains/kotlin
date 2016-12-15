val p1: suspend a
val p2: suspend (a) -> a
val p3: suspend (a) -> suspend a
val p4: suspend a.() -> a
val p4a: @a a.() -> a
val p5: (suspend a).() -> a
val p5a: (@a a).() -> a
val p6: a<in suspend a>
val p7: a<out suspend @a a>
val p8: a<out @a suspend @a a>
val p9: a<out @[a] suspend @[a] a>
val p10: suspend a<a>
val p11: suspend @a a
val p12: @a suspend a
val p13: @a suspend @a a
val p14: @[a] suspend @[a] a
val p15: suspend (suspend (() -> Unit)) -> Unit

@a fun @a a.f1() {}
fun (@a a.(a) -> a).f2() {}
