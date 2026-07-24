// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// JVM_EXPOSE_BOXED

@JvmInline
value class Id(
    val id: Long,
)

class C(
    private val delegate: MutableCollection<Id>,
) : MutableCollection<Id> by delegate

fun box(): String {
    C(arrayListOf<Id>())
    return "OK"
}
