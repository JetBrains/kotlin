@file:Suppress("unused") // usages in build scripts are not tracked properly

import org.gradle.api.*
import org.gradle.api.artifacts.ConfigurablePublishArtifact
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.*
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.Dependency
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.plugins.BasePluginConvention
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.jvm.tasks.Jar
import java.io.File

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

fun Project.buildVersion(): Dependency {
    val cfg = configurations.create("build-version")
    return dependencies.add(cfg.name, dependencies.project(":prepare:build.version", configuration = "buildVersion"))
}

fun<T: Jar> Project.runtimeJar(task: T, body: T.() -> Unit = {}): T {
    val buildVersionCfg = configurations.create("buildVersion")
    dependencies.add(buildVersionCfg.name, dependencies.project(":prepare:build.version", configuration = "buildVersion"))
    extra["runtimeJarTask"] = task
    return task.apply {
        setupPublicJar()
        from(buildVersionCfg) { into("META-INF") }
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        body()
        project.runtimeJarArtifactBy(this, this)
    }
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

fun Project.ideaPlugin(subdir: String = "lib", body: AbstractCopyTask.() -> Unit) {
    task<Copy>("idea-plugin") {
        body()
        into(File(rootProject.extra["ideaPluginDir"].toString(), subdir).path)
        rename("-${java.util.regex.Pattern.quote(rootProject.extra["build.number"].toString())}", "")
    }
}

fun Project.ideaPlugin(subdir: String = "lib") = ideaPlugin(subdir) {
    fromRuntimeJarIfExists(this)
}

fun Project.dist(targetDir: File? = null,
                 targetName: String? = null,
                 fromTask: Task? = null,
                 body: AbstractCopyTask.() -> Unit = {}): AbstractCopyTask {
    val distJarCfg = configurations.getOrCreate("distJar")
    val distLibDir: File by rootProject.extra
    val distJarName = targetName ?: (the<BasePluginConvention>().archivesBaseName + ".jar")

    return task<Copy>("dist") {
        body()
        when {
            fromTask != null -> from(fromTask)
            else -> project.fromRuntimeJarIfExists(this)
        }
        rename(".*", distJarName)
//        rename("-${java.util.regex.Pattern.quote(rootProject.extra["build.number"].toString())}", "")
        into(targetDir ?: distLibDir)
        project.addArtifact(distJarCfg, this, File(targetDir ?: distLibDir, distJarName))
    }
}

private fun<T: AbstractCopyTask> Project.fromRuntimeJarIfExists(task: T) {
    if (extra.has("runtimeJarTask")) {
        task.from(extra["runtimeJarTask"] as Task)
    }
    else {
        tasks.findByName("jar")?.let {
            task.from(it)
        }
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
