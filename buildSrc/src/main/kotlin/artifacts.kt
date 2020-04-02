@file:Suppress("unused") // usages in build scripts are not tracked properly

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.ConfigurablePublishArtifact
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.plugins.BasePluginConvention
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.Upload
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.*


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
    tasks.named("jar").configure {
        enabled = false
        actions = emptyList()
        configurations.forEach { cfg ->
            removeArtifacts(cfg, this)
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

fun Project.runtimeJar(body: Jar.() -> Unit = {}): TaskProvider<Jar> = runtimeJar(getOrCreateTask("jar", body), { })

fun <T : Jar> Project.runtimeJar(task: TaskProvider<T>, body: T.() -> Unit = {}): TaskProvider<T> {
    tasks.named<Jar>("jar").configure {
        removeArtifacts(configurations.getOrCreate("archives"), this)
    }
    task.configure {
        configurations.findByName("embedded")?.let { embedded ->
            dependsOn(embedded)
            from {
                embedded.map(::zipTree)
            }
        }
        setupPublicJar(project.the<BasePluginConvention>().archivesBaseName)
        setDuplicatesStrategy(DuplicatesStrategy.EXCLUDE)
        body()
        project.runtimeJarArtifactBy(this, this)
    }
    return task
}

fun Project.sourcesJar(body: Jar.() -> Unit = {}): TaskProvider<Jar> {
    val task = tasks.register<Jar>("sourcesJar") {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        archiveClassifier.set("sources")

        from(project.mainSourceSet.allSource)

        project.configurations.findByName("embedded")?.let { embedded ->
            from(provider {
                embedded.resolvedConfiguration
                    .resolvedArtifacts
                    .map { it.id.componentIdentifier }
                    .filterIsInstance<ProjectComponentIdentifier>()
                    .mapNotNull {
                        project(it.projectPath)
                            .findJavaPluginConvention()
                            ?.mainSourceSet
                            ?.allSource
                    }
            })
        }

        body()
    }

    addArtifact("archives", task)
    addArtifact("sources", task)

    return task
}

fun Project.javadocJar(body: Jar.() -> Unit = {}): TaskProvider<Jar> {
    val javadocTask = getOrCreateTask<Jar>("javadocJar") {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        archiveClassifier.set("javadoc")
        tasks.findByName("javadoc")?.let { it as Javadoc }?.takeIf { it.enabled }?.let {
            dependsOn(it)
            from(it.destinationDir)
        }
        body()
    }

    addArtifact("archives", javadocTask)
    return javadocTask
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

fun <T : Task> Project.addArtifact(configurationName: String, task: TaskProvider<T>, body: ConfigurablePublishArtifact.() -> Unit = {}) {
    configurations.maybeCreate(configurationName)
    artifacts.add(configurationName, task, body)
}

fun Project.cleanArtifacts() {
    configurations["archives"].artifacts.let { artifacts ->
        artifacts.forEach {
            artifacts.remove(it)
        }
    }
}
