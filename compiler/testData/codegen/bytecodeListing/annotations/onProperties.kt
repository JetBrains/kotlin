// LANGUAGE: +UseGetterNameForPropertyAnnotationsMethodOnJvm
// JVM_ABI_K1_K2_DIFF: KT-63984, KT-69075

import kotlin.reflect.KProperty

annotation class AnnProp
annotation class AnnField
annotation class AnnProp2
annotation class AnnGetter
annotation class AnnSetter
annotation class AnnParam
annotation class AnnDelegate

class CustomDelegate {
    operator fun getValue(thisRef: Any?, prop: KProperty<*>): String = prop.name
}

public class A(@AnnParam @field:AnnField @property:AnnProp2 val x: Int, @param:AnnParam @get:AnnGetter @set:AnnSetter var y: Int) {

    @AnnProp @field:AnnField @property:AnnProp2 @get:AnnGetter @set:AnnSetter @setparam:AnnParam
    var p: Int = 0

    @AnnProp @property:AnnProp2 @delegate:AnnDelegate @property:AnnDelegate
    val s: String by CustomDelegate()

}
