// !LANGUAGE: -ForbidInferringTypeVariablesIntoEmptyIntersection
// FILE: ObjectNode.java

public interface ObjectNode {
    <T extends JsonNode> T set(String fieldName, JsonNode value);
}

// FILE: JsonNode.java

public class JsonNode

// FILE: test.kt

interface JsonObject
class SomeJsonObject() : JsonObject

fun String.put(value: JsonObject?, node: ObjectNode) {
    select(
        node.<!INFERRED_TYPE_VARIABLE_INTO_EMPTY_INTERSECTION_WARNING!>set<!>(this, null),
        Unit,
    )

    if (value == null) node.<!INFERRED_TYPE_VARIABLE_INTO_EMPTY_INTERSECTION_WARNING!>set<!>(this, null)
    else if (value is SomeJsonObject) Unit

    when (value) {
        null -> node.<!INFERRED_TYPE_VARIABLE_INTO_EMPTY_INTERSECTION_WARNING!>set<!>(this, null)
        is SomeJsonObject -> Unit
        else -> TODO()
    }
}

fun TODO(): Nothing = null!!
fun <K> select(vararg values: K): K = values[0]

