import p.Outer
import p.Ref

fun test(ref: Ref): Outer.Inner = ref.get()
