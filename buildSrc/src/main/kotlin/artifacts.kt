@file:Suppress("unused") // usages in build scripts are not tracked properly

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.UnknownDomainObjectException
import org.gradle.api.artifacts.ConfigurablePublishArtifact
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.internal.artifacts.publish.ArchivePublishArtifact
import org.gradle.api.plugins.BasePluginConvention
import org.gradle.api.tasks.AbstractCopyTask
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Upload
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.*
import java.io.File


private const val MAGIC_DO_NOT_CHANGE_TEST_JAR_TASK_NAME = "testJar"

fun Project.testsJar(body: Jar.() -> Unit = {}): Jar {
    val testsJarCfg = configurations.getOrCreate("tests-jar").extendsFrom(configurations["testCompile"])

    return task<Jar>(MAGIC_DO_NOT_CHANGE_TEST_JAR_TASK_NAME) {
        dependsOn("testClasses")
        pluginManager.withPlugin("java") {
            from(testSourceSet.output)
        }
        classifier = "tests"
        body()
        project.addArtifact(testsJarCfg, this, this)
    }
}

var Project.artifactsRemovedDiagnosticFlag: Boolean
    get() = extra.has("artifactsRemovedDiagnosticFlag") && extra["artifactsRemovedDiagnosticFlag"] == true
    set(value) {
        extra["artifactsRemovedDiagnosticFlag"] = value
    }

fun Project.removeArtifacts(configuration: Configuration, task: Task) {
    configuration.artifacts.removeAll { artifact ->
        artifact.file in task.outputs.files
    }

    artifactsRemovedDiagnosticFlag = true
}

fun Project.noDefaultJar() {
    tasks.findByName("jar")?.let { defaultJarTask ->
        defaultJarTask.enabled = false
        defaultJarTask.actions = emptyList()
        configurations.forEach { cfg ->
            removeArtifacts(cfg, defaultJarTask)
        }
    }
}

fun Project.runtimeJarArtifactBy(task: Task, artifactRef: Any, body: ConfigurablePublishArtifact.() -> Unit = {}) {
    addArtifact("archives", task, artifactRef, body)
    addArtifact("runtimeJar", task, artifactRef, body)
    configurations.findByName("runtime")?.let {
        addArtifact(it, task, artifactRef, body)
    }
}

fun <T : Jar> Project.runtimeJar(task: T, body: T.() -> Unit = {}): T {
    extra["runtimeJarTask"] = task
    tasks.findByName("jar")?.let { defaultJarTask ->
        removeArtifacts(configurations.getOrCreate("archives"), defaultJarTask)
    }
    return task.apply {
        setupPublicJar(project.the<BasePluginConvention>().archivesBaseName)
        setDuplicatesStrategy(DuplicatesStrategy.EXCLUDE)
        body()
        project.runtimeJarArtifactBy(this, this)
    }
}

fun Project.runtimeJar(body: Jar.() -> Unit = {}): Jar = runtimeJar(getOrCreateTask("jar", body), { })

fun Project.sourcesJar(sourceSet: String? = "main", body: Jar.() -> Unit = {}): Jar =
    getOrCreateTask("sourcesJar") {
        setDuplicatesStrategy(DuplicatesStrategy.EXCLUDE)
        classifier = "sources"
        try {
            if (sourceSet != null) {
                project.pluginManager.withPlugin("java-base") {
                    from(project.javaPluginConvention().sourceSets[sourceSet].allSource)
                }
            }
        } catch (e: UnknownDomainObjectException) {
            // skip default sources location
        }
        body()
        project.addArtifact("archives", this, this)
    }

fun Project.javadocJar(body: Jar.() -> Unit = {}): Jar = getOrCreateTask("javadocJar") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    classifier = "javadoc"
    tasks.findByName("javadoc")?.let { it as Javadoc }?.takeIf { it.enabled }?.let {
        dependsOn(it)
        from(it.destinationDir)
    }
    body()
    project.addArtifact("archives", this, this)
}


fun Project.standardPublicJars() {
    runtimeJar()
    sourcesJar()
    javadocJar()
}

fun Project.publish(body: Upload.() -> Unit = {}): Upload {
    apply<plugins.PublishedKotlinModule>()

    if (artifactsRemovedDiagnosticFlag) {
        error("`publish()` should be called before removing artifacts typically done in `noDefaultJar()` or `runtimeJar()` call")
    }

    afterEvaluate {
        if (configurations.findByName("classes-dirs") != null)
            throw GradleException("classesDirsArtifact() is incompatible with publish(), see sources comments for details")
    }

    return (tasks.getByName("uploadArchives") as Upload).apply {
        body()
    }
}

fun Project.ideaPlugin(subdir: String = "lib", body: AbstractCopyTask.() -> Unit): Copy {
    val thisProject = this
    val pluginTask = task<Copy>("ideaPlugin") {
        body()
        into(File(rootProject.extra["ideaPluginDir"].toString(), subdir).path)
        rename("-${java.util.regex.Pattern.quote(thisProject.version.toString())}", "")
    }

    task("idea-plugin") {
        dependsOn(pluginTask)
    }

    return pluginTask
}

fun Project.ideaPlugin(subdir: String = "lib"): Copy = ideaPlugin(subdir) {
    runtimeJarTaskIfExists()?.let {
        from(it)
    }
}

fun Project.dist(
    targetDir: File? = null,
    targetName: String? = null,
    fromTask: Task? = null,
    body: AbstractCopyTask.() -> Unit = {}
): AbstractCopyTask {
    val distJarCfg = configurations.getOrCreate("distJar")
    val distLibDir: File by rootProject.extra
    val distJarName = targetName ?: (the<BasePluginConvention>().archivesBaseName + ".jar")
    val thisProject = this

    return task<Copy>("dist") {
        body()
        (fromTask ?: runtimeJarTaskIfExists())?.let {
            from(it)
            if (targetName != null) {
                rename(it.outputs.files.singleFile.name, targetName)
            }
        }
        rename("-${java.util.regex.Pattern.quote(thisProject.version.toString())}", "")
        into(targetDir ?: distLibDir)
        project.addArtifact(distJarCfg, this, File(targetDir ?: distLibDir, distJarName))
    }
}

private fun Project.runtimeJarTaskIfExists(): Task? =
    if (extra.has("runtimeJarTask")) extra["runtimeJarTask"] as Task
    else tasks.findByName("jar")


fun ConfigurationContainer.getOrCreate(name: String): Configuration = findByName(name) ?: create(name)

fun Jar.setupPublicJar(baseName: String, classifier: String = "") {
    val buildNumber = project.rootProject.extra["buildNumber"] as String
    this.baseName = baseName
    this.classifier = classifier
    manifest.attributes.apply {
        put("Implementation-Vendor", "JetBrains")
        put("Implementation-Title", baseName)
        put("Implementation-Version", buildNumber)
    }
}


fun Project.addArtifact(configuration: Configuration, task: Task, artifactRef: Any, body: ConfigurablePublishArtifact.() -> Unit = {}) {
    artifacts.add(configuration.name, artifactRef) {
        builtBy(task)
        body()
    }
}

fun Project.addArtifact(configurationName: String, task: Task, artifactRef: Any, body: ConfigurablePublishArtifact.() -> Unit = {}) =
    addArtifact(configurations.getOrCreate(configurationName), task, artifactRef, body)

fun Project.cleanArtifacts() {
    configurations["archives"].artifacts.let { artifacts ->
        artifacts.forEach {
            artifacts.remove(it)
        }
    }
}
