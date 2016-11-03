// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

//WITH_RUNTIME
//FULL_JDK
//NOTE this test should be removed if Kotlin const properties will became inlined

import java.lang.reflect.Field
import java.lang.reflect.Modifier

object ThingTemplate {
    const val prop = 0
}

class ThingVal {
    val prop = ThingTemplate.prop
}

class ThingVar {
    var prop = ThingTemplate.prop
}


fun box() : String {
    val template = ThingTemplate;
    val javaClass = ThingTemplate::class.java
    val field = javaClass.getDeclaredField("prop")!!
    field.isAccessible = true

    val modifiersField = Field::class.java!!.getDeclaredField("modifiers")
    modifiersField.isAccessible = true
    modifiersField.setInt(field, field.modifiers and Modifier.FINAL.inv())

    field.set(null, 1)

    val thingVal = ThingVal()
    if (thingVal.prop != 1) return "fail 1"

    val thingVar = ThingVar()
    if (thingVar.prop != 1) return "fail 2"

    return "OK"
}