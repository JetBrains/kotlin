import java.io.*

fun box(): String {
    val f : (File?, String?) -> Boolean = { dir, name -> if (name == null) false else (name as java.lang.String).endsWith(".md") }
    val filter = FilenameFilter(f)
    val listFiles = File(".").listFiles(filter)!!
    if (listFiles.size != 1) return "Wrong size: $listFiles.size"
    val name = listFiles[0].getName()
    return if (name == "ReadMe.md") "OK" else "Wrong name: $name"
}
