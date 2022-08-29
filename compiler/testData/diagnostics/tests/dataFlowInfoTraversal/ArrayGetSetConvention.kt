// FILE: Plugin.java
public interface Plugin<T> {}

// FILE: PluginContainer.java
public interface PluginContainer extends PluginCollection<Plugin> {}

// FILE: PluginCollection.java
public interface PluginCollection<T> {}

// FILE: JavaPlugin.java
public class JavaPlugin implements Plugin<String> {}

// FILE: main.kt
fun <S : Any> PluginCollection<in S>.withType() {}

fun foo(p: PluginContainer) {
    p.withType<JavaPlugin>()
}
