import java.io.*

fun box(): String {
    val filter = FilenameFilter { dir, name -> if (name == null) false else (name as java.lang.String).endsWith(".md") }
    val listFiles = File(".").listFiles(filter)!!
    if (listFiles.size != 1) return "Wrong size: $listFiles.size"
    val name = listFiles[0].getName()
    return if (name == "ReadMe.md") "OK" else "Wrong name: $name"
}
