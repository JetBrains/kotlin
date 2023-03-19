// FIR_IDENTICAL
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
    <!INFERRED_TYPE_VARIABLE_INTO_EMPTY_INTERSECTION_WARNING!>when (value) {
        null -> node.set(this, null)
        is SomeJsonObject -> Unit
        else -> TODO()
    }<!>
}

fun TODO(): Nothing = null!!
