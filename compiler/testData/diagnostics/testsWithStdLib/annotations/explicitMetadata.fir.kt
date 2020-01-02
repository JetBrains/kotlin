@Metadata
class A

@Metadata(extraString = "_")
annotation class B(val m: Metadata)

@Metadata(extraInt = 0)
@B(Metadata())
fun f() {}
