// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// JVM_EXPOSE_BOXED

@JvmInline
value class Id(val value: Long)

interface BigArity<T> {
    fun invoke(a1:String, a2:T, a3:T, a4:T, a5:T, a6:T, a7:T, a8:T, a9:T, a10:T, a11:T, a12:T, a13:T,
               a14:T, a15:T, a16:T, a17:T, a18:T, a19:T, a20:T, a21:T, a22:T, a23:T): String
}

class Child: BigArity<Id> {
    override fun invoke(a1:String, a2:Id, a3:Id, a4:Id, a5:Id, a6:Id, a7:Id, a8:Id, a9:Id, a10:Id, a11:Id, a12:Id, a13:Id,
               a14:Id, a15:Id, a16:Id, a17:Id, a18:Id, a19:Id, a20:Id, a21:Id, a22:Id, a23:Id): String = a1
}

fun box(): String {
    val a: BigArity<Id> = Child()
    return a.invoke("OK",
                    Id(1L), Id(1L), Id(1L), Id(1L), Id(1L), Id(1L), Id(1L), Id(1L), Id(1L), Id(1L), Id(1L), Id(1L), Id(1L), Id(1L), Id(1L),
                    Id(1L), Id(1L), Id(1L), Id(1L), Id(1L), Id(1L), Id(1L))
}
