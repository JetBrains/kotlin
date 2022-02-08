// WITH_STDLIB
// FULL_JDK

// FILE: Plugin.java
public interface Plugin {}

// FILE: IdeaPlugin.java
import java.io.File
import java.util.Set

public interface IdeaPlugin extends Plugin {
    Set<File> getExcludeDirs()
}

// FILE: PluginContainer.java
import javax.annotation.Nullable

public interface PluginContainer {
    @Nullable
    <T extends Plugin> T findPlugin(Class<T> type);
}

// FILE: Project.java
public interface Project {
    PluginContainer getPlugins();

    Project getRootProject();

    File getBuildDir();
}

// FILE: main.kt
import java.io.File

fun getExcludedDirs(project: Project, excludedProjects: List<Project>): List<File> {
    return project.plugins.findPlugin(IdeaPlugin::class.java)?.excludeDirs?.toList() ?: emptyList()
}
