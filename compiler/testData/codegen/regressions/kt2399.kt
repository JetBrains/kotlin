import java.util.ArrayList

class JsonObject {
}

class JsonArray {
}

class ProjectInfo {
    public fun toString(): String = "OK"
}

public trait Parser<in IN: Any, out OUT: Any> {
    public fun parse(source: IN): OUT
}

public trait MultiParser<in IN: Any, out OUT: Any> {
    public fun parse(source: IN): Collection<OUT>
}

public trait JsonParser<T: Any>: Parser<JsonObject, T>, MultiParser<JsonArray, T> {
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
