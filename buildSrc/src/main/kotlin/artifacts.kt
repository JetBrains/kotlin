@file:Suppress("unused") // usages in build scripts are not tracked properly

import org.gradle.api.*
import org.gradle.api.artifacts.ConfigurablePublishArtifact
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.*
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.jvm.tasks.Jar


fun Project.testsJar(body: Jar.() -> Unit = {}): Jar {
    val testsJarCfg = configurations.getOrCreate("tests-jar").extendsFrom(configurations["testCompile"])

    return task<Jar>("testsJar") {
        dependsOn("testClasses")
        pluginManager.withPlugin("java") {
            from(project.the<JavaPluginConvention>().sourceSets.getByName("test").output)
        }
        classifier = "tests"
        body()
        project.addArtifact(testsJarCfg, this, this)
    }
}

fun<T> Project.runtimeJarArtifactBy(task: Task, artifactRef: T, body: ConfigurablePublishArtifact.() -> Unit = {}) {
    addArtifact("archives", task, artifactRef, body)
    addArtifact("runtimeJar", task, artifactRef, body)
}

fun<T: Jar> Project.runtimeJar(task: T, body: T.() -> Unit = {}): T =
        task.apply {
            setupPublicJar()
            body()
            project.runtimeJarArtifactBy(this, this)
        }

fun Project.runtimeJar(taskName: String = "jar", body: Jar.() -> Unit = {}): Jar = runtimeJar(getOrCreateTask(taskName, body))

fun Project.sourcesJar(body: Jar.() -> Unit = {}): Jar =
        getOrCreateTask("sourcesJar") {
            setupPublicJar("Sources")
            try {
                project.pluginManager.withPlugin("java-base") {
                    from(project.the<JavaPluginConvention>().sourceSets["main"].allSource)
                }
            } catch (e: UnknownDomainObjectException) {
                // skip default sources location
            }
            tasks.findByName("classes")?.let { dependsOn(it) }
            body()
            project.addArtifact("archives", this, this)
        }

fun Project.javadocJar(body: Jar.() -> Unit = {}): Jar =
        getOrCreateTask("javadocJar") {
            setupPublicJar("JavaDoc")
            tasks.findByName("javadoc")?.let{ it as Javadoc }?.takeIf { it.enabled }?.let {
                dependsOn(it)
                from(it.destinationDir)
            }
            body()
            project.addArtifact("archives", this, this)
        }


fun Project.standardPublicJars(): Unit {
    runtimeJar()
    sourcesJar()
    javadocJar()
}

fun Project.publish(body: Upload.() -> Unit = {}): Upload {
    apply<plugins.PublishedKotlinModule>()

    return (tasks.getByName("uploadArchives") as Upload).apply {
        body()
    }
}

fun Project.ideaPlugin(subdir: String = "lib", body: Copy.() -> Unit) {
    task<Copy>("idea-plugin") {
        body()
        into(File(rootProject.extra["ideaPluginDir"].toString(), subdir).path)
        rename("-${java.util.regex.Pattern.quote(rootProject.extra["build.number"].toString())}", "")
    }
}

fun Project.ideaPlugin() = ideaPlugin {
    tasks.findByName("jar")?.let {
        from(it)
    }
}


fun Project.dist(body: Copy.() -> Unit) {
    task<Copy>("dist") {
        body()
        rename("-${java.util.regex.Pattern.quote(rootProject.extra["build.number"].toString())}", "")
        into(rootProject.extra["distLibDir"].toString())
    }
}

fun Project.dist() = dist {
    tasks.findByName("jar")?.let {
        from(it)
    }
}

fun ConfigurationContainer.getOrCreate(name: String): Configuration = findByName(name) ?: create(name)

fun Jar.setupPublicJar(classifier: String = "", classifierDescr: String? = null) {
    this.classifier = classifier.toLowerCase()
    dependsOn(":prepare:build.version:prepare")
    manifest.attributes.apply {
        put("Built-By", project.rootProject.extra["manifest.impl.vendor"])
        put("Implementation-Vendor", project.rootProject.extra["manifest.impl.vendor"])
        put("Implementation-Title", "${project.description} ${classifierDescr ?: classifier}".trim())
        put("Implementation-Version", project.rootProject.extra["build.number"])
    }
//    from(project.configurations.getByName("build-version").files, action = { into("META-INF/") })
}


fun<T> Project.addArtifact(configuration: Configuration, task: Task, artifactRef: T, body: ConfigurablePublishArtifact.() -> Unit = {}) {
    artifacts.add(configuration.name, artifactRef) {
        builtBy(task)
        body()
    }
}

fun<T> Project.addArtifact(configurationName: String, task: Task, artifactRef: T, body: ConfigurablePublishArtifact.() -> Unit = {}) =
        addArtifact(configurations.getOrCreate(configurationName), task, artifactRef, body)

inline fun<reified T: Task> Project.getOrCreateTask(taskName: String, body: T.() -> Unit): T =
        (tasks.findByName(taskName)?.let { it as T } ?: task<T>(taskName)).apply{ body() }
