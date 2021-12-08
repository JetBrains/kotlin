// KOTLIN_CONFIGURATION_FLAGS: STRING_CONCAT=inline
// WITH_STDLIB

import kotlin.test.assertEquals

fun concatAny(x: Any) = "$x!!"
fun <T : String> concat1(x: T) = "[[$x]]"
fun <T : String?> concat2(x: T) = "[[$x]]"
fun <T : String> concat3(x: T?) = "[[$x]]"
fun <T : String> concat4(x: T) = x + "!!"
fun <T : String?> concat5(x: T) = x + "!!"
fun <T : String> concat6(x: T?) = x + "!!"

fun box(): String {
    assertEquals("[[1]]", concat1("1"))
    assertEquals("[[2]]", concat2("2"))
    assertEquals("[[null]]", concat2(null))
    assertEquals("[[3]]", concat3("3"))
    assertEquals("[[null]]", concat3(null))
    assertEquals("4!!", concat4("4"))
    assertEquals("5!!", concat5("5"))
    assertEquals("null!!", concat5("null"))
    assertEquals("6!!", concat5("6"))
    assertEquals("null!!", concat5("null"))

    return "OK"
}

// CHECK_BYTECODE_TEXT
// JVM_IR_TEMPLATES
// 1 INVOKEVIRTUAL java/lang/StringBuilder\.append \(Ljava/lang/Object;\)Ljava/lang/StringBuilder;
//  ^ single instance of 'StringBuilder.append(Object)' from 'concatAny',
//    keep it here to make sure there's no error in regexp.
// 16 INVOKEVIRTUAL java/lang/StringBuilder\.append \(Ljava/lang/String;\)Ljava/lang/StringBuilder;
//  ^ everything else is done with 'StringBuilder.append(String)'
// 17 INVOKEVIRTUAL java/lang/StringBuilder\.append
//  ^ no other instances of StringBuidler.append(...)
