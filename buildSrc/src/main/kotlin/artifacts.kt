@file:Suppress("unused") // usages in build scripts are not tracked properly

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.gradle.publish.PublishTask
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.ConfigurablePublishArtifact
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.PublishArtifact
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.attributes.LibraryElements
import org.gradle.api.attributes.Usage
import org.gradle.api.component.AdhocComponentWithVariants
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.plugins.BasePluginExtension
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPlugin.*
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.tasks.GenerateModuleMetadata
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetContainer
import plugins.KotlinBuildPublishingPlugin
import plugins.mainPublicationName


private const val MAGIC_DO_NOT_CHANGE_TEST_JAR_TASK_NAME = "testJar"

fun Project.testsJar(body: Jar.() -> Unit = {}): Jar {
    val testsJarCfg = configurations.getOrCreate("tests-jar").extendsFrom(configurations["testApi"])

    return task<Jar>(MAGIC_DO_NOT_CHANGE_TEST_JAR_TASK_NAME) {
        dependsOn("testClasses")
        pluginManager.withPlugin("java") {
            from(testSourceSet.output)
        }
        archiveClassifier.set("tests")
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
        configurations.forEach { cfg ->
            removeArtifacts(cfg, this)
        }
    }
}

fun Project.runtimeJar(body: Jar.() -> Unit = {}): TaskProvider<out Jar> {
    val jarTask = tasks.named<Jar>("jar")
    jarTask.configure {
        configurations.findByName("embedded")?.let { embedded ->
            dependsOn(embedded)
            from {
                embedded.map(::zipTree)
            }
        }
        setupPublicJar(project.extensions.getByType<BasePluginExtension>().archivesName.get())
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        body()
    }

    return jarTask
}

fun Project.runtimeJar(task: TaskProvider<ShadowJar>, body: ShadowJar.() -> Unit = {}): TaskProvider<out Jar> {

    noDefaultJar()

    task.configure {
        configurations = configurations + listOf(project.configurations["embedded"])
        setupPublicJar(project.extensions.getByType<BasePluginExtension>().archivesName.get())
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        body()
    }

    project.addArtifact("archives", task, task)
    project.addArtifact("runtimeElements", task, task)
    project.addArtifact("apiElements", task, task)

    return task
}

fun Project.sourcesJar(body: Jar.() -> Unit = {}): TaskProvider<Jar> {
    configure<JavaPluginExtension> {
        withSourcesJar()
    }

    val sourcesJar = getOrCreateTask<Jar>("sourcesJar") {
        fun Project.mainJavaPluginSourceSet() = findJavaPluginExtension()?.sourceSets?.findByName("main")
        fun Project.mainKotlinSourceSet() =
            (extensions.findByName("kotlin") as? KotlinSourceSetContainer)?.sourceSets?.findByName("main")

        fun Project.sources() = mainJavaPluginSourceSet()?.allSource ?: mainKotlinSourceSet()?.kotlin

        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        archiveClassifier.set("sources")

        from(project.sources())

        project.configurations.findByName("embedded")?.let { embedded ->
            from(provider {
                embedded.resolvedConfiguration
                    .resolvedArtifacts
                    .map { it.id.componentIdentifier }
                    .filterIsInstance<ProjectComponentIdentifier>()
                    .mapNotNull {
                        project(it.projectPath).sources()
                    }
            })
        }

        body()
    }

    addArtifact("archives", sourcesJar)
    addArtifact("sources", sourcesJar)

    configurePublishedComponent {
        addVariantsFromConfiguration(configurations[SOURCES_ELEMENTS_CONFIGURATION_NAME]) { }
    }

    return sourcesJar
}

fun Project.javadocJar(body: Jar.() -> Unit = {}): TaskProvider<Jar> {
    configure<JavaPluginExtension> {
        withJavadocJar()
    }

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

    configurePublishedComponent {
        addVariantsFromConfiguration(configurations[JAVADOC_ELEMENTS_CONFIGURATION_NAME]) { }
    }

    return javadocTask
}

fun Project.modularJar(body: Jar.() -> Unit): TaskProvider<Jar> {
    val modularJar = configurations.maybeCreate("modularJar").apply {
        isCanBeConsumed = true
        isCanBeResolved = false
        attributes {
            attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
            attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named("modular-jar"))
        }
    }

    val modularJarTask = getOrCreateTask<Jar>("modularJar") {
        archiveClassifier.set("modular")

        body()
    }

    addArtifact("modularJar", modularJarTask)
    addArtifact("archives", modularJarTask)

    configurePublishedComponent {
        addVariantsFromConfiguration(modularJar) { mapToMavenScope("runtime") }
    }

    return modularJarTask
}


fun Project.standardPublicJars() {
    runtimeJar()
    sourcesJar()
    javadocJar()
}

fun Project.publish(moduleMetadata: Boolean = false, configure: MavenPublication.() -> Unit = { }) {
    apply<KotlinBuildPublishingPlugin>()

    if (!moduleMetadata) {
        tasks.withType<GenerateModuleMetadata> {
            enabled = false
        }
    }

    val publication = extensions.findByType<PublishingExtension>()
        ?.publications
        ?.findByName(mainPublicationName) as MavenPublication
    publication.configure()
}

fun Project.publishGradlePlugin() {
    mainPublicationName = "pluginMaven"
    publish()

    afterEvaluate {
        tasks.withType<PublishTask> {
            // Makes plugin publication task reuse poms and metadata from publication named "pluginMaven"
            useAutomatedPublishing()
            useGradleModuleMetadataIfAvailable()
        }
    }
}

fun Project.idePluginDependency(block: () -> Unit) {
    val shouldActivate = rootProject.findProperty("publish.ide.plugin.dependencies")?.toString()?.toBoolean() == true
    if (shouldActivate) {
        block()
    }
}

fun Project.publishJarsForIde(projects: List<String>, libraryDependencies: List<String> = emptyList()) {
    idePluginDependency {
        publishProjectJars(projects, libraryDependencies)
    }
    configurations.all {
        // Don't allow `ideaIC` from compiler to leak into Kotlin plugin modules. Compiler and
        // plugin may depend on different versions of IDEA and it will lead to version conflict
        exclude(module = ideModuleName())
    }
    dependencies {
        projects.forEach {
            jpsLikeJarDependency(project(it), JpsDepScope.COMPILE, { isTransitive = false }, exported = true)
        }
        libraryDependencies.forEach {
            jpsLikeJarDependency(it, JpsDepScope.COMPILE, exported = true)
        }
    }
}

fun Project.publishTestJarsForIde(projectNames: List<String>) {
    idePluginDependency {
        // Compiler test infrastructure should not affect test running in IDE.
        // If required, the components should be registered on the IDE plugin side.
        val excludedPaths = listOf("junit-platform.properties", "META-INF/services")
        publishTestJar(projectNames, excludedPaths)
    }
    configurations.all {
        // Don't allow `ideaIC` from compiler to leak into Kotlin plugin modules. Compiler and
        // plugin may depend on different versions of IDEA and it will lead to version conflict
        exclude(module = ideModuleName())
    }
    dependencies {
        for (projectName in projectNames) {
            jpsLikeJarDependency(projectTests(projectName), JpsDepScope.COMPILE, exported = true)
        }
    }
}

fun Project.publishProjectJars(projects: List<String>, libraryDependencies: List<String> = emptyList()) {
    apply<JavaPlugin>()

    val fatJarContents by configurations.creating

    dependencies {
        for (projectName in projects) {
            fatJarContents(project(projectName)) { isTransitive = false }
        }

        for (libraryDependency in libraryDependencies) {
            fatJarContents(libraryDependency)
        }
    }

    publish()

    val jar: Jar by tasks

    jar.apply {
        dependsOn(fatJarContents)

        from {
            fatJarContents.map(::zipTree)
        }
    }

    sourcesJar {
        from {
            projects.map {
                project(it).mainSourceSet.allSource
            }
        }
    }

    javadocJar()
}

fun Project.publishTestJar(projects: List<String>, excludedPaths: List<String>) {
    apply<JavaPlugin>()

    val fatJarContents by configurations.creating

    dependencies {
        for (projectName in projects) {
            fatJarContents(project(projectName, configuration = "tests-jar")) { isTransitive = false }
        }
    }

    publish()

    val jar: Jar by tasks

    jar.apply {
        dependsOn(fatJarContents)

        from {
            fatJarContents.map(::zipTree)
        }

        exclude { treeElement ->
            val path = treeElement.path
            excludedPaths.any { path == it || path.startsWith("$it/") }
        }
    }

    sourcesJar {
        from {
            projects.map { project(it).testSourceSet.allSource }
        }
    }

    javadocJar()
}

fun ConfigurationContainer.getOrCreate(name: String): Configuration = findByName(name) ?: create(name)

fun Jar.setupPublicJar(baseName: String, classifier: String = "") {
    val buildNumber = project.rootProject.extra["buildNumber"] as String
    this.archiveBaseName.set(baseName)
    this.archiveClassifier.set(classifier)
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

fun <T : Task> Project.addArtifact(
    configurationName: String,
    task: TaskProvider<T>,
    body: ConfigurablePublishArtifact.() -> Unit = {}
): PublishArtifact {
    configurations.maybeCreate(configurationName)
    return artifacts.add(configurationName, task, body)
}

fun <T : Task> Project.addArtifact(
    configurationName: String,
    task: TaskProvider<T>,
    artifactRef: Any,
    body: ConfigurablePublishArtifact.() -> Unit = {}
): PublishArtifact {
    configurations.maybeCreate(configurationName)
    return artifacts.add(configurationName, artifactRef) {
        builtBy(task)
        body()
    }
}

fun Project.cleanArtifacts() {
    configurations["archives"].artifacts.let { artifacts ->
        artifacts.forEach {
            artifacts.remove(it)
        }
    }
}

fun Project.configurePublishedComponent(configure: AdhocComponentWithVariants.() -> Unit) =
    (components.findByName(KotlinBuildPublishingPlugin.ADHOC_COMPONENT_NAME) as AdhocComponentWithVariants?)?.apply(configure)
