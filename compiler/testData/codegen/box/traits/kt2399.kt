// IGNORE_BACKEND_K1: WASM
// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS
// on JS Parser.parse and MultiParser.parse clash in ProjectInfoJsonParser

class JsonObject {
}

class JsonArray {
}

class ProjectInfo {
    override fun toString(): String = "OK"
}

public interface Parser<in IN: Any, out OUT: Any> {
    public fun parse(source: IN): OUT
}

public interface MultiParser<in IN: Any, out OUT: Any> {
    public fun parse(source: IN): Collection<OUT>
}

public interface JsonParser<T: Any>: Parser<JsonObject, T>, MultiParser<JsonArray, T> {
    public override fun parse(source: JsonArray): Collection<T> {
        return ArrayList<T>()
    }
}

public abstract class ProjectInfoJsonParser(): JsonParser<ProjectInfo> {
    public override fun parse(source: JsonObject): ProjectInfo {
        return ProjectInfo()
    }
}

class ProjectApiContext {
    public val projectInfoJsonParser: ProjectInfoJsonParser = object : ProjectInfoJsonParser(){
    }
}

fun box(): String {
    val context = ProjectApiContext()
    val array = context.projectInfoJsonParser.parse(JsonArray())
    return if (array != null) "OK" else "fail"
}
