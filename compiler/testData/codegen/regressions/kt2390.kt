import java.util.Collection

class JsonObject() {
}

class JsonArray() {
}

public trait Formatter<in IN: Any, out OUT: Any> {
    public fun format(source: IN?): OUT
}

public trait MultiFormatter <in IN: Any, out OUT: Any> {
    public fun format(source: Collection<IN>): OUT
}

public class Project() {
}

public trait JsonFormatter<in IN: Any>: Formatter<IN, JsonObject>, MultiFormatter<IN, JsonArray> {
    public override fun format(source: Collection<IN>): JsonArray {
        return JsonArray();
    }
}

public class ProjectJsonFormatter(): JsonFormatter<Project> {
   public override fun format(source: Project?): JsonObject {
        return JsonObject()
    }
}


fun box(): String {
  val formatter = ProjectJsonFormatter()
  return if (formatter.format(Project()) != null) "OK" else "fail"
}
