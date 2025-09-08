// LIBRARY_PLATFORMS: JVM
val commands = java.util.HashMap<String, () -> Unit>()   // multiple errors

class Lifetime{
 val attached = ArrayList<()->Unit>()
}
