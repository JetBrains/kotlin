import java.io.File
import java.io.Serializable

val File.name: String
    get() = getName()

val Serializable.name: String
    get() = ""

val File.parent: File
    get() = getParentFile()

class MyFile : File("")

val MyFile.isFile: Boolean
    get() = isFile()

@Deprecated("hidden", level = DeprecationLevel.HIDDEN)
val Thread.priority: Int
    get() = getPriority()
