package stdlibDelegatedProperty

import kotlin.properties.Delegates

var prop: Int by Delegates.notNull()

fun main(args: Array<String>) {
    prop = 3
    val a = prop
}

// ADDITIONAL_BREAKPOINT: Delegation.kt:public override fun set(thisRef: Any?, desc: PropertyMetadata, value: T) {

// EXPRESSION: value.toString()
// RESULT: "3": Ljava/lang/String;