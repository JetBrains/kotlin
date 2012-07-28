// "Specify Type Explicitly" "true"
import java.util.Map

fun getEntry() : java.util.Map.Entry<jet.Array<String>, java.sql.Array> {
}

val x: Map.Entry<Array<String>, java.sql.Array> = getEntry()