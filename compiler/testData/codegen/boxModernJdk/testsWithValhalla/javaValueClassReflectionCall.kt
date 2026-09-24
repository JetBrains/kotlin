// WITH_REFLECT

// Only JVM preview features are enabled here, so `LocalDate`, `Optional` and `JavaVal` are value classes at runtime. Unlike Kotlin
// inline value classes, they are passed as is, so reflection calls the callables taking them as usual.

// FILE: JavaVal.java
public value class JavaVal {
    public final int x;

    public JavaVal(int x) {
        this.x = x;
    }
}

// FILE: test.kt
import java.time.LocalDate
import java.util.Optional
import kotlin.reflect.full.primaryConstructor

@JvmInline
value class KInline(val x: Int)

class WithLocalDate(val d: LocalDate)
class WithOptional(val o: Optional<String>)
class WithJavaVal(val j: JavaVal)
data class Event(val name: String, val date: LocalDate)

fun mixed(k: KInline, d: LocalDate): Int = k.x + d.year
fun LocalDate.plusInline(k: KInline): LocalDate = plusDays(k.x.toLong())

fun box(): String {
    if (!LocalDate::class.isValue || !JavaVal::class.isValue) return "isValue"
    val date = LocalDate.of(2020, 1, 2)
    val withLocalDate = WithLocalDate::class.primaryConstructor!!
    if (withLocalDate.call(date).d != date) return "WithLocalDate.call"
    if (withLocalDate.callBy(mapOf(withLocalDate.parameters.single() to date)).d != date) return "WithLocalDate.callBy"
    if (WithOptional::class.primaryConstructor!!.call(Optional.of("o")).o.get() != "o") return "WithOptional"
    if (WithJavaVal::class.primaryConstructor!!.call(JavaVal(1)).j.x != 1) return "WithJavaVal"
    if (Event::class.primaryConstructor!!.call("e", date) != Event("e", date)) return "Event"
    if (::mixed.call(KInline(1), date) != 2021) return "mixed"
    if (LocalDate::plusInline.call(date, KInline(1)) != LocalDate.of(2020, 1, 3)) return "plusInline"
    if (date::plusInline.call(KInline(2)) != LocalDate.of(2020, 1, 4)) return "bound plusInline"
    return "OK"
}
