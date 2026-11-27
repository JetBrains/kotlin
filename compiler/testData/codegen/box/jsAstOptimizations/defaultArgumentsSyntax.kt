// ISSUE: KT-89592
// TARGET_BACKEND: JS_IR_ES6
// ONLY_IR_DCE
// CHECK_OPTIMIZED_JS
// ENABLE_UNUSED_PROPERTY_DCE

// FILE: withAninymousObject.kt
@JsExport
interface Producer<T> {
    fun produce(): T
}

fun withAnonymousObject(
    producer1: Producer<String> = object : Producer<String> { override fun produce(): String = "Makise" },
    producer2: Producer<String> = object : Producer<String> { override fun produce(): String = "Kurisu" },
): String = producer1.produce() + " " + producer2.produce()

// FILE: topLevelConstInitializer.kt

private val theAnswer = 42

fun triggeringTopLevelConstInitializer(x: Int = theAnswer, y: Int, z: Int = theAnswer): String = "x=$x, y=$y, z=$z"

// FILE: topLevelPureInitializer.kt

private val theAnswer = identity(42)

fun triggeringTopLevelPureInitializer(x: Int = theAnswer, y: Int, z: Int = theAnswer): String = "x=$x, y=$y, z=$z"

// FILE: topLevelEffectfulInitializer.kt

private val theAnswerWithEffect = run {
    log += "theAnswerWithEffect;"
    42
}

fun triggeringTopLevelEffectfulInitializer(x: Int = theAnswerWithEffect, y: Int, z: Int = theAnswerWithEffect): String = "x=$x, y=$y, z=$z"

// FILE: referencingOtherParameters.kt
fun referencingOtherParameters(x: Int, y: Int = x + 1, z: Int = y * 2): String = "x=$x, y=$y, z=$z"

// FILE: conditionalInDefaultValue.kt
fun simpleConditional(condition: Boolean, x: Int = if (condition) 1 else -1): String = "x=$x"

fun complexConditional(
    x: Int,
    y: Int = when (x) {
        1 -> {
            log += "complexConditional;"
            0
        }
        else -> x + 1
    }
): String = "x=$x, y=$y"

// FILE: abstractClass.kt

@JsExport
abstract class Animal {
    abstract val defaultPrefix: String
    abstract fun say(prefix: String = defaultPrefix): String
}

@JsExport
class Cat: Animal() {
    override val defaultPrefix get() = ""
    override fun say(prefix: String) = prefix + "nya!"
}

// FILE: main.kt
var log = ""

// FUNCTION_HAS_EFFECTS: function=identity PURE
fun <T> identity(x: T) = x

// EXPECT_GENERATED_JS: expect=defaultArgumentSyntax.out.js function=withAnonymousObject;triggeringTopLevelConstInitializer;triggeringTopLevelPureInitializer;triggeringTopLevelEffectfulInitializer;referencingOtherParameters;simpleConditional;complexConditional class=Animal;Cat
fun box(): String {
    assertEquals("Makise Kurisu", withAnonymousObject())
    assertEquals(
        "Okabe Rintaro",
         withAnonymousObject(
            object : Producer<String> { override fun produce(): String = "Okabe" },
            object : Producer<String> { override fun produce(): String = "Rintaro" },
         )
    )
    assertEquals("x=42, y=100, z=42", triggeringTopLevelConstInitializer(y = 100))
    assertEquals("x=1337, y=100, z=2", triggeringTopLevelConstInitializer(1337, 100, 2))
    assertEquals("x=42, y=100, z=42", triggeringTopLevelPureInitializer(y = 100))
    assertEquals("x=1337, y=100, z=2", triggeringTopLevelPureInitializer(1337, 100, 2))
    assertEquals("", log);
    assertEquals("x=42, y=100, z=42", triggeringTopLevelEffectfulInitializer(y = 100))
    assertEquals("theAnswerWithEffect;", log);
    assertEquals("x=1337, y=100, z=2", triggeringTopLevelEffectfulInitializer(1337, 100, 2))
    assertEquals("x=1, y=2, z=4", referencingOtherParameters(1))
    assertEquals("x=10, y=11, z=12", referencingOtherParameters(10, 11, 12))
    assertEquals("x=1", simpleConditional(true))
    assertEquals("x=-1", simpleConditional(false))
    assertEquals("x=42", simpleConditional(true, 42))
    assertEquals("x=42", simpleConditional(false, 42))
    assertEquals("x=1, y=0", complexConditional(1))
    assertEquals("theAnswerWithEffect;complexConditional;", log);
    assertEquals("x=2, y=3", complexConditional(2))
    assertEquals("x=2, y=100", complexConditional(2, 100))
    assertEquals("nya!", Cat().say())
    assertEquals("hello nya!", Cat().say("hello "))
    return "OK"
}

