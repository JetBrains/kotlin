// LANGUAGE: +ContextParameters +ExplicitContextArguments
// WITH_STDLIB
import kotlin.test.assertEquals

var state = ""

fun context1(): Int = 1.also { state += "context1;" }
fun context2(): Int = 2.also { state += "context2;" }
fun value(): String = "".also { state += "value;" }
fun value1(): String = "".also { state += "value1;" }
fun value2(): String = "".also { state += "value2;" }
fun extension(): Double = 0.0.also { state += "extension;" }
fun result(): String = state.also { state = "" }

context(c: Int)
fun f1(v: String): String = result()

context(c1: Int, c2: Int)
fun f2(v: String): String = result()

context(c: Int)
fun Double.f3(v1: String, v2: String): String = result()

fun box(): String {
    assertEquals("context1;value;", f1(c = context1(), v = value()))
    assertEquals("value;context1;", f1(value(), c = context1()))

    assertEquals("context1;context2;value;", f2(c1 = context1(), c2 = context2(), v = value()))
    assertEquals("context1;value;context2;", f2(c1 = context1(), v = value(), c2 = context2()))
    assertEquals("context2;context1;value;", f2(c2 = context2(), c1 = context1(), v = value()))
    assertEquals("context2;value;context1;", f2(c2 = context2(), v = value(), c1 = context1()))
    assertEquals("value;context1;context2;", f2(value(), c1 = context1(), c2 = context2()))
    assertEquals("value;context2;context1;", f2(value(), c2 = context2(), c1 = context1()))
    assertEquals("extension;context1;value1;value2;", extension().f3(c = context1(), v1 = value1(), v2 = value2()))
    assertEquals("extension;context1;value2;value1;", extension().f3(c = context1(), v2 = value2(), v1 = value1()))
    assertEquals("extension;value1;context1;value2;", extension().f3(v1 = value1(), c = context1(), v2 = value2()))
    assertEquals("extension;value1;context1;value2;", extension().f3(value1(), c = context1(), v2 = value2()))
    assertEquals("extension;value1;value2;context1;", extension().f3(v1 = value1(), v2 = value2(), c = context1()))
    assertEquals("extension;value1;value2;context1;", extension().f3(v1 = value1(), value2(), c = context1()))
    assertEquals("extension;value1;value2;context1;", extension().f3(value1(), value2(), c = context1()))
    assertEquals("extension;value1;value2;context1;", extension().f3(value1(), v2 = value2(), c = context1()))

    assertEquals("extension;value2;context1;value1;", extension().f3(v2 = value2(), c = context1(), v1 = value1()))
    assertEquals("extension;value2;value1;context1;", extension().f3(v2 = value2(), v1 = value1(), c = context1()))

    return "OK"
}
