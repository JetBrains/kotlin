// WITH_RUNTIME
// SUGGESTED_RETURN_TYPES: kotlin.Boolean?, kotlin.Boolean
// PARAM_DESCRIPTOR: value-parameter val it: kotlin.Map.Entry<(kotlin.Boolean..kotlin.Boolean?), (kotlin.Boolean..kotlin.Boolean?)> defined in test.<anonymous>
// PARAM_TYPES: kotlin.Map.Entry<(kotlin.Boolean..kotlin.Boolean?), (kotlin.Boolean..kotlin.Boolean?)>
fun test() {
    J.getMap().filter { <selection>it.getKey()</selection> }
}