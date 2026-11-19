import p.Gen
import p.Ref

fun testRaw(ref: Ref): Gen<String> = ref.raw()
